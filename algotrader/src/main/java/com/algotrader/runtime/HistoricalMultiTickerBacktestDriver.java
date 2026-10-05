package com.algotrader.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.decision.dataobjects.*;
import com.algotrader.execution.*;
import com.algotrader.logging.*;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.portfolio.PortfolioValuator;
import com.algotrader.registry.OpenTradeAdjustment;
import com.algotrader.registry.OpenTradeRegistry;

/** Executes historical cycles and retains partial results and marked portfolio states. */
public final class HistoricalMultiTickerBacktestDriver {
    private final MultiTickerTradingCycleEvaluator evaluator;
    private final HistoricalCycleClock cycleClock;
    private final TradeExecutor tradeExecutor;
    private final PortfolioManager portfolioManager;
    private final OpenTradeRegistry openTradeRegistry;
    private final MultiTickerTradingSessionLogger logger;
    private final PortfolioValuator valuator;
    private final RunArtifacts artifacts;

    public HistoricalMultiTickerBacktestDriver(MultiTickerTradingCycleEvaluator evaluator,
            HistoricalCycleClock cycleClock, TradeExecutor tradeExecutor,
            PortfolioManager portfolioManager, OpenTradeRegistry openTradeRegistry,
            MultiTickerTradingSessionLogger logger, PortfolioValuator valuator, RunArtifacts artifacts) {
        this.evaluator = Objects.requireNonNull(evaluator);
        this.cycleClock = Objects.requireNonNull(cycleClock);
        this.tradeExecutor = Objects.requireNonNull(tradeExecutor);
        this.portfolioManager = Objects.requireNonNull(portfolioManager);
        this.openTradeRegistry = Objects.requireNonNull(openTradeRegistry);
        this.logger = Objects.requireNonNull(logger);
        this.valuator = Objects.requireNonNull(valuator);
        this.artifacts = Objects.requireNonNull(artifacts);
    }

    public TradingSessionLog run() {
        long driverStart = System.nanoTime();
        Instant lastTime = cycleClock.getStartTime();
        logger.start(portfolioManager.cash());
        logger.identify(artifacts.runId(), valuator.value(portfolioManager.snapshot(), lastTime, "INITIAL"));
        Exception failure = null;
        boolean liquidationAttempted = false;
        try {
            artifacts.modelProvenance(evaluator.provenance());
            evaluator.initialize();
            int count = 0;
            while (cycleClock.hasNext()) {
                lastTime = cycleClock.next();
                MultiTickerTradingCycleLog cycle = runCycle(lastTime);
                logger.logCycle(cycle);
                if (cycle.cycleFailure() != null) {
                    throw new TradingSessionException("Cycle failed at " + lastTime + " during " + cycle.failureStage());
                }
                if (++count % 100 == 0) System.out.printf("Progress: %d/%d%n", count, cycleClock.getTotalCycles());
            }
            // Never retroactively sell at a price preceding a cycle already processed.
            liquidationAttempted = true;
            liquidateRemainingTrades(lastTime);
        } catch (Exception e) {
            failure = e;
        }
        logger.finalState(valuator.value(portfolioManager.snapshot(), lastTime,
                liquidationAttempted ? "POST_FINAL_LIQUIDATION_ATTEMPT" : "AFTER_FAILURE"),
                openTradeRegistry.getOpenTrades());
        TradingSessionLog session = logger.finish(portfolioManager.cash(), failure);
        long driverNanos = System.nanoTime() - driverStart;
        long writeStart = System.nanoTime();
        new TradingSessionLogWriter().write(session, artifacts.directory().resolve("session.json"));
        long writeNanos = System.nanoTime() - writeStart;
        try {
            artifacts.complete(session, driverNanos, writeNanos);
        } catch (java.io.IOException e) {
            throw new TradingSessionException("Unable to write run completion marker", e);
        }
        System.out.println("Run artifact: " + artifacts.directory().toAbsolutePath());
        return session;
    }

    private MultiTickerTradingCycleLog runCycle(Instant time) {
        long start = System.nanoTime();
        long stageStart = start;
        long exitNanos = 0, evaluationNanos = 0, entryNanos = 0;
        String stage = "EXITS";
        String failureStage = null;
        CycleFailureLog failure = null;
        var due = openTradeRegistry.getTradesDueForExit(time);
        List<RoundTripTrade> closed = new ArrayList<>(), opened = new ArrayList<>();
        List<TradeExecutionResult> exits = new ArrayList<>(), entries = new ArrayList<>();
        List<FailedTradeExecution> failedExits = new ArrayList<>(), failedEntries = new ArrayList<>();
        List<OpenTradeAdjustment> appliedAdjustments = new ArrayList<>();
        MultiTickerCycleEvaluation evaluation = null;
        List<CapitalAllocation> allocations = List.of();
        try {
            for (var trade : due) {
                var instruction = new SellInstruction(trade.ticker(), trade.quantity(), time);
                TradeExecutionResult fill;
                try {
                    fill = tradeExecutor.handleInstruction(instruction).withTradeId(trade.tradeId());
                } catch (TradeExecutionException e) {
                    failedExits.add(new FailedTradeExecution(instruction, e.getMessage(), trade.tradeId()));
                    continue;
                }
                exits.add(fill);
                portfolioManager.apply(fill);
                valuator.observe(fill);
                openTradeRegistry.close(trade);
                closed.add(trade);
            }
            exitNanos = System.nanoTime() - stageStart;
            stage = "EVALUATION";
            stageStart = System.nanoTime();
            evaluation = evaluator.evaluate(time);
            evaluationNanos = System.nanoTime() - stageStart;
            allocations = evaluation.newCapitalAllocations();
            stage = "ENTRIES_AND_ADJUSTMENTS";
            stageStart = System.nanoTime();
            for (var allocation : allocations) {
                String tradeId = UUID.randomUUID().toString();
                var instruction = new BuyInstruction(allocation.ticker(), allocation.cashAmount(), time);
                TradeExecutionResult fill;
                try {
                    fill = tradeExecutor.handleInstruction(instruction).withTradeId(tradeId);
                } catch (TradeExecutionException e) {
                    failedEntries.add(new FailedTradeExecution(instruction, e.getMessage(), tradeId));
                    continue;
                }
                entries.add(fill);
                portfolioManager.apply(fill);
                valuator.observe(fill);
                var trade = new RoundTripTrade(allocation.ticker(), fill.quantity(), time,
                        allocation.plannedExitTime(), allocation.strategyId(), tradeId);
                openTradeRegistry.add(trade);
                opened.add(trade);
            }
            for (var adjustment : evaluation.openTradeAdjustments()) {
                openTradeRegistry.adjust(adjustment);
                appliedAdjustments.add(adjustment);
            }
            entryNanos = System.nanoTime() - stageStart;
        } catch (Exception e) {
            failure = CycleFailureLog.from(e);
            failureStage = stage;
            long elapsed = System.nanoTime() - stageStart;
            switch (stage) {
                case "EXITS" -> exitNanos = elapsed;
                case "EVALUATION" -> evaluationNanos = elapsed;
                default -> entryNanos = elapsed;
            }
        }
        long valueStart = System.nanoTime();
        var value = valuator.value(portfolioManager.snapshot(), time,
                failure == null ? "POST_CYCLE" : "PARTIAL_CYCLE_AFTER_FAILURE");
        long valueNanos = System.nanoTime() - valueStart;
        return new MultiTickerTradingCycleLog(time, evaluation, due, closed, exits, failedExits,
                allocations, opened, entries, failedEntries, appliedAdjustments, failure, failureStage,
                value, new CycleTiming(System.nanoTime() - start, exitNanos, evaluationNanos, entryNanos, valueNanos));
    }

    private void liquidateRemainingTrades(Instant time) {
        var remaining = openTradeRegistry.getOpenTrades();
        List<TradeExecutionResult> fills = new ArrayList<>();
        List<FailedTradeExecution> failures = new ArrayList<>();
        try {
            for (var trade : remaining) {
                var instruction = new SellInstruction(trade.ticker(), trade.quantity(), time);
                TradeExecutionResult fill;
                try {
                    fill = tradeExecutor.handleInstruction(instruction).withTradeId(trade.tradeId());
                } catch (TradeExecutionException e) {
                    failures.add(new FailedTradeExecution(instruction, e.getMessage(), trade.tradeId()));
                    continue;
                }
                fills.add(fill);
                portfolioManager.apply(fill);
                valuator.observe(fill);
                openTradeRegistry.close(trade);
            }
        } finally {
            logger.logEndOfSessionLiquidation(new ForcedLiquidationResult(
                    time, remaining, fills, failures, !openTradeRegistry.hasOpenTrades()));
        }
    }
}
