package com.algotrader.logging;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.decision.dataobjects.*;
import com.algotrader.decision.generator.*;
import com.algotrader.execution.*;
import com.algotrader.marketcalendar.UsMarketCalendar;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.*;
import com.algotrader.marketdata.provider.*;
import com.algotrader.portfolio.*;
import com.algotrader.registry.OpenTradeRegistry;
import com.algotrader.runtime.*;

class HistoricalRunArtifactTest {
    private static final Instant START = Instant.parse("2026-05-18T14:05:00Z");
    private static final Instant END = START.plusSeconds(300);
    @TempDir Path temp;

    @Test void currentStaleAndMissingMarksNeverTurnHoldingsIntoLosses() {
        PriceProvider prices = (ticker, time) -> {
            if (!time.equals(START)) throw new DataCacheException("No candle");
            return new MarketPrice(ticker, 100, time);
        };
        var valuator = new PortfolioValuator(prices);
        var snapshot = new PortfolioSnapshot(500, Map.of("AAPL", 5.0));
        var first = valuator.value(snapshot, START, "POST_CYCLE");
        assertEquals(1000.0, first.totalEquity());
        assertEquals(500.0, first.positions().get("AAPL").value());
        var stale = valuator.value(snapshot, END, "POST_CYCLE");
        assertEquals(1000.0, stale.totalEquity());
        assertEquals("STALE", stale.status());
        assertEquals(START, stale.positions().get("AAPL").priceTimestamp());
        var unknown = new PortfolioValuator(prices).value(snapshot, END, "POST_CYCLE");
        assertNull(unknown.totalEquity());
        assertNull(unknown.positions().get("AAPL").value());
        assertEquals("INCOMPLETE", unknown.status());
        assertEquals(500.0, valuator.value(new PortfolioSnapshot(500, Map.of()), END, "FINAL").totalEquity());
    }

    @Test void futurePricesCannotValueEarlierHoldings() {
        var valuator = new PortfolioValuator((ticker, time) -> new MarketPrice(ticker, 200, END));
        assertNull(valuator.value(new PortfolioSnapshot(500, Map.of("AAPL", 5.0)), START, "POST_CYCLE").totalEquity());
    }

    @Test void finalStateIncludesForcedLiquidationAndLinkedRetry() throws Exception {
        var run = run(false, false);
        var session = run.session;
        assertNull(session.sessionFailure());
        assertEquals(1000.0, session.cycleLogs().get(0).portfolioAfterCycle().totalEquity());
        assertEquals(500.0, session.cycleLogs().get(0).portfolioAfterCycle().cash());
        assertEquals(1050.0, session.finalPortfolio().totalEquity());
        assertEquals(1050.0, session.endingCash());
        assertEquals("POST_FINAL_LIQUIDATION_ATTEMPT", session.finalPortfolio().samplingPoint());
        assertTrue(session.finalPortfolio().positions().isEmpty());
        String id = session.cycleLogs().get(0).successfullyOpenedTrades().get(0).tradeId();
        assertEquals(id, session.cycleLogs().get(0).successfulEntryExecutions().get(0).tradeId());
        assertEquals(id, session.cycleLogs().get(1).failedExitExecutions().get(0).tradeId());
        assertEquals(id, session.liquidationResult().successfulExecutions().get(0).tradeId());
        assertEquals(END, session.liquidationResult().liquidationTime());
        assertTrue(session.cycleLogs().get(0).timing().totalNanos() > 0);
        assertEquals("SKIPPED_EMPTY_WINDOWS", session.cycleLogs().get(0).evaluation().inferenceStatus());
        var json = new ObjectMapper();
        var output = json.readTree(run.artifacts.directory().resolve("session.json").toFile());
        assertEquals(RunArtifacts.SCHEMA, output.path("schemaVersion").asText());
        assertEquals(1050, output.path("finalPortfolio").path("totalEquity").asDouble());
        var completion = json.readTree(run.artifacts.directory().resolve("completion.json").toFile());
        assertEquals("COMPLETED", completion.path("status").asText());
        assertTrue(completion.path("fullBacktestWallNanos").asLong() >= completion.path("driverNanos").asLong());
        assertEquals(RunArtifacts.sha256(run.artifacts.directory().resolve("session.json")), completion.path("sessionSha256").asText());
    }

    @Test void failedLiquidationRetainsValuedHoldingsAndNonCleanStatus() throws Exception {
        var run = run(true, false);
        assertNull(run.session.sessionFailure());
        assertEquals(500, run.session.endingCash());
        assertEquals(1050.0, run.session.finalPortfolio().totalEquity());
        assertEquals(1, run.session.remainingOpenTrades().size());
        assertFalse(run.session.liquidationResult().registryEmptyAfter());
        assertEquals(1, run.session.liquidationResult().failedExecutions().size());
        assertEquals("COMPLETED_WITH_OPEN_POSITIONS", new ObjectMapper().readTree(
                run.artifacts.directory().resolve("completion.json").toFile()).path("status").asText());
    }

    @Test void evaluationFailureAfterExitPreservesPartialCycleAndFinalCash() throws Exception {
        var run = run(false, true);
        var session = run.session;
        assertNotNull(session.sessionFailure());
        assertEquals(2, session.cycleLogs().size());
        var failedCycle = session.cycleLogs().get(1);
        assertEquals("EVALUATION", failedCycle.failureStage());
        assertEquals(1, failedCycle.successfulExitExecutions().size());
        assertEquals("PARTIAL_CYCLE_AFTER_FAILURE", failedCycle.portfolioAfterCycle().samplingPoint());
        assertEquals(1050.0, session.finalPortfolio().totalEquity());
        assertNull(session.liquidationResult());
        assertEquals("FAILED", new ObjectMapper().readTree(run.artifacts.directory().resolve("completion.json").toFile()).path("status").asText());
    }

    @Test void extensionPreservesStableTradeIdentity() {
        var registry = new OpenTradeRegistry(policy(END.plusSeconds(3600)));
        var trade = new RoundTripTrade("AAPL", 5, START, END, "test");
        registry.add(trade);
        registry.adjust(new com.algotrader.registry.OpenTradeAdjustment(trade, END.plusSeconds(300), "test"));
        assertEquals(trade.tradeId(), registry.getOpenTrades().get(0).tradeId());
        assertEquals(START, registry.getOpenTrades().get(0).entryTime());
    }

    private Run run(boolean failAllSells, boolean failEvaluation) throws Exception {
        var artifacts = new RunArtifacts(temp);
        var portfolio = new PortfolioManager(new PortfolioState(1000));
        var registry = new OpenTradeRegistry(policy(END));
        MultiTickerWindowProvider windows = new MultiTickerWindowProvider() {
            public void initialize() {}
            public List<DataBatch> windowsAt(Instant time) { return List.of(); }
        };
        PortfolioDecisionGenerator decisions = (batches, snapshot, trades, time) -> {
            if (time.equals(START)) return new PortfolioDecisionResult(
                    List.of(new CapitalAllocation("AAPL", 500, START, END, "test")), List.of());
            if (failEvaluation) throw new IllegalStateException("Synthetic failure after exits");
            return new PortfolioDecisionResult(List.of(), List.of());
        };
        PriceProvider prices = (ticker, time) -> new MarketPrice(ticker, time.equals(START) ? 100 : 110, time);
        var paper = new HistoricalPaperTradeExecutor(prices);
        AtomicInteger sells = new AtomicInteger();
        TradeExecutor executor = instruction -> {
            if (instruction instanceof SellInstruction && !failEvaluation
                    && (failAllSells || sells.getAndIncrement() == 0)) throw new TradeExecutionException("Synthetic missing fill");
            return paper.handleInstruction(instruction);
        };
        var evaluator = new MultiTickerTradingCycleEvaluator(windows, decisions, portfolio, registry);
        var driver = new HistoricalMultiTickerBacktestDriver(evaluator,
                new HistoricalCycleClock(START, END, TimeInterval.FIVE_MINUTES,
                        new UsMarketCalendar(ZoneId.of("America/New_York"), LocalTime.of(9, 30),
                                LocalTime.of(16, 0), Set.of(), Map.of())), executor, portfolio, registry,
                new MultiTickerTradingSessionLogger(), new PortfolioValuator(prices), artifacts);
        return new Run(driver.run(), artifacts);
    }

    static TradeExitTimePolicy policy(Instant end) {
        return new TradeExitTimePolicy(new UsMarketCalendar(ZoneId.of("America/New_York"),
                LocalTime.of(9, 30), LocalTime.of(16, 0), Set.of(), Map.of()), TimeInterval.FIVE_MINUTES, end);
    }
    private record Run(TradingSessionLog session, RunArtifacts artifacts) {}
}
