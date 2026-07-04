package com.algotrader.runtime;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.decision.dataobjects.BuyInstruction;
import com.algotrader.decision.dataobjects.CapitalAllocation;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.dataobjects.SellInstruction;
import com.algotrader.decision.dataobjects.TradeInstruction;
import com.algotrader.execution.TradeExecutionException;
import com.algotrader.execution.TradeExecutionResult;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.logging.MultiTickerCycleEvaluation;
import com.algotrader.logging.MultiTickerTradingCycleLog;
import com.algotrader.logging.MultiTickerTradingSessionLogger;
import com.algotrader.logging.TradeExecutionFailureLog;
import com.algotrader.logging.TradingSessionLog;
import com.algotrader.logging.TradingSessionLogWriter;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.registry.OpenTradeAdjustment;
import com.algotrader.registry.OpenTradeRegistry;

/**
 * Drives a historical multi-ticker backtest.
 *
 * <p>This class owns the historical cycle loop and performs the side effects
 * intentionally excluded from {@link MultiTickerTradingCycleEvaluator}:
 *
 * <ul>
 *     <li>executing scheduled exits</li>
 *     <li>executing new capital allocations</li>
 *     <li>mutating the portfolio model</li>
 *     <li>mutating the open trade registry</li>
 *     <li>logging cycle outcomes</li>
 * </ul>
 */
public final class HistoricalMultiTickerBacktestDriver {

        private final MultiTickerTradingCycleEvaluator evaluator;
        private final HistoricalCycleClock cycleClock;
        private final TradeExecutor tradeExecutor;
        private final PortfolioManager portfolioManager;
        private final OpenTradeRegistry openTradeRegistry;
        private final MultiTickerTradingSessionLogger logger;

        /**
         * Creates a historical multi-ticker backtest driver.
         *
         * @param evaluator evaluator used to produce cycle decisions
         * @param cycleClock clock that supplies historical cycle timestamps
         * @param tradeExecutor executor used to simulate trade instructions
         * @param portfolioManager manager updated after successful executions
         * @param openTradeRegistry registry of trades currently open
         * @param logger session logger that records cycle outcomes
         * @throws IllegalArgumentException if any dependency is null
         */
        public HistoricalMultiTickerBacktestDriver(
                MultiTickerTradingCycleEvaluator evaluator,
                HistoricalCycleClock cycleClock,
                TradeExecutor tradeExecutor,
                PortfolioManager portfolioManager,
                OpenTradeRegistry openTradeRegistry,
                MultiTickerTradingSessionLogger logger
        ) {
                if (evaluator == null) {
                throw new IllegalArgumentException("Evaluator cannot be null.");
                }

                if (cycleClock == null) {
                throw new IllegalArgumentException("Cycle clock cannot be null.");
                }

                if (tradeExecutor == null) {
                throw new IllegalArgumentException("Trade executor cannot be null.");
                }

                if (portfolioManager == null) {
                throw new IllegalArgumentException("Portfolio manager cannot be null.");
                }

                if (openTradeRegistry == null) {
                throw new IllegalArgumentException("Open trade registry cannot be null.");
                }

                if (logger == null) {
                throw new IllegalArgumentException("Logger cannot be null.");
                }

                this.evaluator = evaluator;
                this.cycleClock = cycleClock;
                this.tradeExecutor = tradeExecutor;
                this.portfolioManager = portfolioManager;
                this.openTradeRegistry = openTradeRegistry;
                this.logger = logger;
        }

        /**
         * Runs the historical session to completion.
         *
         * @return completed session log, including any session-level failure
         * @throws TradingSessionException if evaluator initialization fails
         */
        public TradingSessionLog run() {
                try {
                        evaluator.initialize();
                } catch (DataCacheException e) {
                        throw new TradingSessionException(
                                "Failed to initialize trading session.",
                                e
                        );
                }

                logger.start(portfolioManager.cash());

                int cycle = 0;
                int totalCycles = cycleClock.getTotalCycles();

                try {
                        while (cycleClock.hasNext()) {
                                Instant cycleTime = cycleClock.next();

                                MultiTickerTradingCycleLog cycleLog =
                                        runCycle(cycleTime);

                                logger.logCycle(cycleLog);

                                cycle++;
                                if (cycle % 100 == 0) {
                                        double percent = 100.0 * cycle / totalCycles;
                                        System.out.printf("Progress: %.1f%% (%d/%d)%n",
                                                percent,
                                                cycle,
                                                totalCycles);
                                }
                        }

                        TradingSessionLog sessionLog = logger.finish(portfolioManager.cash());

                        new TradingSessionLogWriter().write(
                                sessionLog,
                                Path.of("data", "logs", "latest-session.json")
                        );

                        return sessionLog;

                } catch (Exception e) {
                        TradingSessionLog sessionLog = logger.finish(portfolioManager.cash(), e);

                        new TradingSessionLogWriter().write(
                                sessionLog,
                                Path.of("data", "logs", "latest-session.json")
                        );

                        return sessionLog;
                }
        }

        private MultiTickerTradingCycleLog runCycle(Instant cycleTime) {
                List<RoundTripTrade> tradesDueForExit =
                        openTradeRegistry.getTradesDueForExit(cycleTime);

                List<RoundTripTrade> successfullyClosedTrades = new ArrayList<>();
                List<TradeExecutionResult> successfulExitExecutions = new ArrayList<>();
                List<TradeExecutionFailureLog> failedExitExecutions = new ArrayList<>();

                for (RoundTripTrade trade : tradesDueForExit) {
                        TradeInstruction exitInstruction = new SellInstruction(
                                trade.ticker(),
                                trade.quantity(),
                                cycleTime
                        );

                        try {
                                //TODO: in theory this step might take time, delaying calcluation
                                // and allocation of new positions. If trading real trading is ever
                                // implemented, this setup needs to change
                                TradeExecutionResult execution =
                                        tradeExecutor.handleInstruction(exitInstruction);

                                portfolioManager.apply(execution);

                                successfulExitExecutions.add(execution);
                                successfullyClosedTrades.add(trade);

                        } catch (TradeExecutionException e) {
                                failedExitExecutions.add(TradeExecutionFailureLog.from(e));
                        }
                }

                openTradeRegistry.closeAll(successfullyClosedTrades);

                MultiTickerCycleEvaluation evaluation =
                        evaluator.evaluate(cycleTime);

                List<CapitalAllocation> newCapitalAllocations =
                        evaluation.newCapitalAllocations();

                List<TradeExecutionResult> successfulEntryExecutions = new ArrayList<>();
                List<RoundTripTrade> successfullyOpenedTrades = new ArrayList<>();
                List<TradeExecutionFailureLog> failedEntryExecutions = new ArrayList<>();

                for (CapitalAllocation allocation : newCapitalAllocations) {
                        TradeInstruction entryInstruction = new BuyInstruction(
                                allocation.ticker(),
                                allocation.cashAmount(),
                                cycleTime
                );

                        try {
                                TradeExecutionResult execution =
                                        tradeExecutor.handleInstruction(entryInstruction);

                                portfolioManager.apply(execution);

                                RoundTripTrade openedTrade = new RoundTripTrade(
                                        allocation.ticker(),
                                        execution.quantity(),
                                        cycleTime,
                                        allocation.plannedExitTime(),
                                        allocation.strategyId()
                                );

                                successfulEntryExecutions.add(execution);
                                successfullyOpenedTrades.add(openedTrade);

                        } catch (TradeExecutionException e) {
                                failedEntryExecutions.add(TradeExecutionFailureLog.from(e));
                        }
                }

                openTradeRegistry.addAll(successfullyOpenedTrades);

                List<OpenTradeAdjustment> adjustments =
                        evaluation.openTradeAdjustments();

                openTradeRegistry.adjustAll(adjustments);

                return new MultiTickerTradingCycleLog(
                        cycleTime,
                        evaluation,
                        tradesDueForExit,
                        successfullyClosedTrades,
                        successfulExitExecutions,
                        failedExitExecutions,
                        newCapitalAllocations,
                        successfullyOpenedTrades,
                        successfulEntryExecutions,
                        failedEntryExecutions,
                        adjustments,
                        null
                );
        }
}
