package com.algotrader.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.algotrader.config.*;
import com.algotrader.marketcalendar.UsMarketCalendar;
import com.algotrader.marketdata.model.*;
import com.algotrader.persistence.sqlite.repository.SQLiteOHLCVRepository;

class HistoricalDataCoverageValidatorTest {
    @TempDir Path temp;
    private static final Instant OPEN = Instant.parse("2026-07-07T13:30:00Z");
    private static final Instant START = OPEN.plusSeconds(31 * 300);
    private static final Instant END = Instant.parse("2026-07-08T19:55:00Z");
    private final UsMarketCalendar calendar = new UsMarketCalendar(ZoneId.of("America/New_York"),
            LocalTime.of(9, 30), LocalTime.of(16, 0), Set.of(LocalDate.parse("2026-11-26")),
            Map.of(LocalDate.parse("2026-11-27"), LocalTime.of(13, 0)));

    @Test void fullUniverseCoverageCountsActualWindowsAndExpectedMorningWarmup() throws Exception {
        var repo = data(null);
        var result = new HistoricalDataCoverageValidator(repo, calendar).validate(plan(START, END));
        assertEquals(125, result.marketCycles());
        assertEquals(94, result.fullUniverseInferenceCycles());
        assertEquals(31, result.warmupCycles());
        assertEquals(OPEN, result.availableByTicker().get("AAPL").firstCandleOpen());
    }

    @Test void requestedMonthsBeyondDataFailsWithRequestedAndActualRanges() {
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(data(null), calendar)
                        .validate(plan(START, Instant.parse("2026-09-29T19:55:00Z"))));
        assertTrue(error.getMessage().contains("2026-09-29T19:55:00Z"));
        assertTrue(error.getMessage().contains(END.toString()));
        assertTrue(error.getMessage().contains("AAPL"));
        assertTrue(error.getMessage().contains("does not cover"));
    }

    @Test void firstCycleRequiresThirtyOneCompletedCandlesNotCurrentCandle() {
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(data(null), calendar).validate(plan(START.minusSeconds(300), END)));
        assertTrue(error.getMessage().contains("Insufficient completed lookback"));
        assertTrue(error.getMessage().contains("31 contiguous"));
    }

    @Test void missingFirstLookbackCandleForOneTickerIsDetectedDespiteAdequateExtrema() {
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(data(OPEN.plusSeconds(300)), calendar).validate(plan(START, END)));
        assertTrue(error.getMessage().contains("[MSFT]"));
        assertTrue(error.getMessage().contains("Insufficient completed lookback"));
    }

    @Test void internalGapAndMissingFinalExecutionAreRejected() {
        var gap = Instant.parse("2026-07-08T14:00:00Z");
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(data(gap), calendar).validate(plan(START, END)));
        assertTrue(error.getMessage().contains("Missing execution candle for MSFT at " + gap));
        var finalError = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(data(END), calendar).validate(plan(START, END)));
        assertTrue(finalError.getMessage().contains("MSFT does not cover"));
    }

    @Test void boundaryAtMarketCloseIsNotAnExecutableCandle() {
        var repo = data(null);
        for (String ticker : List.of("AAPL", "MSFT")) repo.save(candle(ticker, END.plusSeconds(300)));
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(repo, calendar).validate(plan(START, END.plusSeconds(300))));
        assertTrue(error.getMessage().contains("is not executable"));
    }

    @Test void epochSentinelsDoNotFalselyExpandGenuineCoverage() {
        var repo = data(null);
        for (String ticker : List.of("AAPL", "MSFT")) repo.save(candle(ticker, Instant.ofEpochSecond(1)));
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(repo, calendar).validate(plan(OPEN.minusSeconds(86400), END)));
        assertTrue(error.getMessage().contains("firstCandleOpen=" + OPEN));
        assertFalse(error.getMessage().contains("1970"));
    }

    @Test void nonFiniteExecutionPricesFailBeforeSimulation() {
        var repo = data(null);
        repo.save(new StampedOHLCV("AAPL", TimeInterval.FIVE_MINUTES, START,
                new OHLCV(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 99, 100, 1000)));
        var error = assertThrows(IllegalArgumentException.class, () ->
                new HistoricalDataCoverageValidator(repo, calendar).validate(plan(START, END)));
        assertTrue(error.getMessage().contains("Invalid OHLC prices for AAPL at " + START));
    }

    @Test void earlyCloseHasElevenInferenceWindowsAndExecutableTerminalPrice() throws Exception {
        var repo = new SQLiteOHLCVRepository(temp.resolve("early.db").toString());
        var open = Instant.parse("2026-11-27T14:30:00Z");
        for (String ticker : List.of("AAPL", "MSFT")) {
            for (int i = 0; i < 42; i++) repo.save(candle(ticker, open.plusSeconds(i * 300)));
        }
        var result = new HistoricalDataCoverageValidator(repo, calendar)
                .validate(plan(open.plusSeconds(31 * 300), Instant.parse("2026-11-27T17:55:00Z")));
        assertEquals(11, result.marketCycles());
        assertEquals(11, result.fullUniverseInferenceCycles());
        assertEquals(0, result.warmupCycles());
    }

    private SQLiteOHLCVRepository data(Instant missingMsft) {
        var repo = new SQLiteOHLCVRepository(temp.resolve(UUID.randomUUID() + ".db").toString());
        List<StampedOHLCV> rows = new ArrayList<>();
        for (String ticker : List.of("AAPL", "MSFT")) {
            for (int day = 0; day < 2; day++) {
                for (int i = 0; i < 78; i++) {
                    var time = OPEN.plusSeconds(day * 86400 + i * 300);
                    if (!ticker.equals("MSFT") || !time.equals(missingMsft)) rows.add(candle(ticker, time));
                }
            }
        }
        repo.saveAll(rows);
        return repo;
    }

    private StampedOHLCV candle(String ticker, Instant time) {
        return new StampedOHLCV(ticker, TimeInterval.FIVE_MINUTES, time, new OHLCV(100, 101, 99, 100, 1000));
    }

    private ResolvedTradingPlan plan(Instant start, Instant end) {
        var session = new TradingSessionConfig("test", "test", "test", List.of("AAPL", "MSFT"), start, end, 1000);
        var endpoint = new EndpointConfig("test", PredictionType.BATCH_CLASSIFICATION_WITH_VOLATILITY,
                URI.create("http://localhost/predict"), "test", "1", TimeInterval.FIVE_MINUTES, 31, Map.of());
        var strategy = new TradeGeneratorConfig("test", TradeGeneratorType.THRESHOLD, "test", "1",
                new TradeGeneratorConfig.StrategyParameters(Map.of()));
        return new ResolvedTradingPlan(session, endpoint, strategy);
    }
}
