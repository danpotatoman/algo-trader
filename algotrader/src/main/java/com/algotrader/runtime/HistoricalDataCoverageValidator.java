package com.algotrader.runtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.marketdata.cache.MarketDataCache;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.provider.WindowProviderFactory;
import com.algotrader.marketdata.source.SQLiteOHLCVSource;
import com.algotrader.persistence.OHLCVRepository;

/** Rejects missing historical inputs and execution prices before inference or simulation starts. */
public final class HistoricalDataCoverageValidator {
    private static final Instant EARLIEST_CONTEMPORARY_DATA = Instant.parse("2000-01-01T00:00:00Z");
    private final OHLCVRepository repository;
    private final MarketCalendar calendar;

    public HistoricalDataCoverageValidator(OHLCVRepository repository, MarketCalendar calendar) {
        this.repository = java.util.Objects.requireNonNull(repository);
        this.calendar = java.util.Objects.requireNonNull(calendar);
    }

    public record AvailableRange(Instant firstCandleOpen, Instant lastCandleOpen, int candles) {}
    public record Coverage(Instant requestedStart, Instant requestedEnd, int requiredCompletedCandles,
            int marketCycles, int fullUniverseInferenceCycles, int warmupCycles,
            Map<String, AvailableRange> availableByTicker) {}

    public Coverage validate(ResolvedTradingPlan plan) throws Exception {
        Instant start = plan.getFirstCandleTimestamp();
        Instant end = plan.getLastCandleTimestamp();
        var interval = plan.getInterval();
        var step = interval.getDuration();
        var lookback = step.multipliedBy(plan.getNumCandles());
        Map<String, AvailableRange> available = new LinkedHashMap<>();
        for (String ticker : plan.getTickers()) {
            // Exclude legacy epoch-sentinel rows from the diagnostic range.
            List<StampedOHLCV> rows = repository.findAll(ticker, interval).stream()
                    .filter(row -> !row.candleOpenTime().isBefore(EARLIEST_CONTEMPORARY_DATA)
                            && row.candleOpenTime().getNano() == 0
                            && Math.floorMod(row.candleOpenTime().getEpochSecond(), step.toSeconds()) == 0)
                    .toList();
            available.put(ticker, rows.isEmpty() ? new AvailableRange(null, null, 0)
                    : new AvailableRange(rows.get(0).candleOpenTime(),
                            rows.get(rows.size() - 1).candleOpenTime(), rows.size()));
        }
        // Check bounds first so a months-too-long request reports its actual data limits.
        for (var entry : available.entrySet()) {
            var range = entry.getValue();
            if (range.candles() == 0 || start.isBefore(range.firstCandleOpen())
                    || end.isAfter(range.lastCandleOpen())) {
                throw failure(plan, available, entry.getKey() + " does not cover the requested evaluation range");
            }
        }
        for (Instant bound : List.of(start, end)) {
            if (!calendar.isTradingTime(bound) || bound.isAfter(calendar.getLastExecutableTime(bound, interval))) {
                throw failure(plan, available, "Boundary " + bound
                        + " is not executable; use market candle opens, ending no later than the last executable candle");
            }
        }
        HistoricalCycleClock clock;
        try {
            clock = new HistoricalCycleClock(start, end, interval, calendar);
        } catch (IllegalArgumentException e) {
            throw failure(plan, available, e.getMessage());
        }
        var cache = new MarketDataCache(new SQLiteOHLCVSource(repository));
        var windows = new WindowProviderFactory(cache).createMultiTickerWindowProvider(plan);
        windows.initialize();
        int inferenceCycles = 0;
        int warmupCycles = 0;
        while (clock.hasNext()) {
            Instant time = clock.next();
            for (String ticker : plan.getTickers()) {
                var executionRows = cache.requestRange(ticker, interval, time, time);
                if (executionRows.isEmpty()) {
                    throw failure(plan, available, "Missing execution candle for " + ticker + " at " + time);
                }
                requireUsable(executionRows.get(0), plan, available);
            }
            // Use the runtime window provider itself, including its completed-candle convention.
            var batches = windows.windowsAt(time);
            if (batches.size() == plan.getTickers().size()) {
                for (var batch : batches) {
                    for (var row : batch.getRows()) requireUsable(row, plan, available);
                }
                inferenceCycles++;
            } else if (!time.equals(start) && batches.isEmpty()
                    && time.minus(lookback).isBefore(calendar.getMarketOpen(time))) {
                // Overnight bars are deliberately not fabricated or stitched into model inputs.
                warmupCycles++;
            } else {
                var usable = batches.stream().map(batch -> batch.getRows().get(0).ticker()).toList();
                var missing = plan.getTickers().stream().filter(ticker -> !usable.contains(ticker)).toList();
                throw failure(plan, available, "Insufficient completed lookback for " + missing + " at " + time
                        + ": require " + plan.getNumCandles() + " contiguous " + interval + " candle opens from "
                        + time.minus(lookback) + " through " + time.minus(step)
                        + ". The first cycle must have a complete window; later session mornings are warm-up only");
            }
        }
        return new Coverage(start, end, plan.getNumCandles(), clock.getTotalCycles(), inferenceCycles,
                warmupCycles, java.util.Collections.unmodifiableMap(available));
    }

    private void requireUsable(StampedOHLCV row, ResolvedTradingPlan plan, Map<String, AvailableRange> available) {
        if (!Double.isFinite(row.open()) || !Double.isFinite(row.high()) || !Double.isFinite(row.low())
                || !Double.isFinite(row.close()) || row.low() <= 0 || row.open() <= 0 || row.close() <= 0) {
            throw failure(plan, available, "Invalid OHLC prices for " + row.ticker() + " at " + row.candleOpenTime());
        }
    }

    private IllegalArgumentException failure(ResolvedTradingPlan plan, Map<String, AvailableRange> available,
            String reason) {
        return new IllegalArgumentException("Historical coverage validation failed: " + reason
                + ". Requested [" + plan.getFirstCandleTimestamp() + ", " + plan.getLastCandleTimestamp()
                + "] (inclusive execution times), interval=" + plan.getInterval()
                + ", completed lookback=" + plan.getNumCandles() + ". Available candle-open ranges: " + available);
    }
}
