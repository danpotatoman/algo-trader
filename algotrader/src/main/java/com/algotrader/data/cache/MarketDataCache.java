package com.algotrader.data.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.MarketDataKey;
import com.algotrader.data.dataobjects.StampedOHLCV;
import com.algotrader.data.provider.MarketDataProvider;
import com.algotrader.data.source.OHLCVSource;

/**
 * Caches OHLCV market data loaded from a single {@link OHLCVSource}.
 *
 * <p>This class provides range-based access to market data while avoiding
 * redundant source queries. It does not construct batches, perform
 * sliding-window traversal, or manage trading-cycle execution.
 */
public class MarketDataCache implements MarketDataProvider {

    private final OHLCVSource source;

    private final Map<MarketDataKey, CacheEntry> cache = new HashMap<>();

    public MarketDataCache(OHLCVSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null.");
        }

        this.source = source;
    }

    @Override
    public List<StampedOHLCV> requestRange(
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime
    ) throws DataCacheException {

        validateRangeRequest(ticker, interval, startTime, endTime);

        String normalizedTicker = ticker.toUpperCase();
        MarketDataKey key = new MarketDataKey(normalizedTicker, interval);

        CacheEntry entry = cache.computeIfAbsent(
                key,
                ignored -> new CacheEntry()
        );

        ensureRangeLoaded(
                entry,
                normalizedTicker,
                interval,
                startTime,
                endTime
        );

        return new ArrayList<>(
                entry.rows
                        .subMap(startTime, true, endTime, true)
                        .values()
        );
    }

    @Override
    public StampedOHLCV requestRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException {

        if (timestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }

        List<StampedOHLCV> rows = requestRange(
                ticker,
                interval,
                timestamp,
                timestamp
        );

        if (rows.isEmpty()) {
            throw new DataCacheException(
                    "No candle found for ticker "
                            + ticker.toUpperCase()
                            + ", interval "
                            + interval
                            + ", timestamp "
                            + timestamp
            );
        }

        return rows.get(0);
    }

    private void ensureRangeLoaded(
            CacheEntry entry,
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime
    ) throws DataCacheException {

        List<TimeRange> missingRanges = findMissingRanges(
                entry.loadedRanges,
                new TimeRange(startTime, endTime)
        );

        for (TimeRange missingRange : missingRanges) {
            List<StampedOHLCV> loadedRows = source.loadRange(
                    ticker,
                    interval,
                    missingRange.start(),
                    missingRange.end()
            );

            if (loadedRows != null) {
                for (StampedOHLCV row : loadedRows) {
                    entry.rows.put(row.timestamp(), row);
                }
            }

            /*
             * Mark the requested missing range as loaded even if the source
             * returned no rows. This prevents repeated queries for weekends,
             * holidays, or genuinely empty ranges.
             */
            entry.loadedRanges.add(missingRange);
            entry.loadedRanges.sort(TimeRange::compareByStart);
            entry.loadedRanges = mergeRanges(entry.loadedRanges);
        }
    }

    private List<TimeRange> findMissingRanges(
            List<TimeRange> loadedRanges,
            TimeRange requestedRange
    ) {
        List<TimeRange> missingRanges = new ArrayList<>();

        Instant cursor = requestedRange.start();

        List<TimeRange> sortedLoadedRanges = new ArrayList<>(loadedRanges);
        sortedLoadedRanges.sort(TimeRange::compareByStart);

        for (TimeRange loadedRange : sortedLoadedRanges) {
            if (loadedRange.end().isBefore(cursor)) {
                continue;
            }

            if (loadedRange.start().isAfter(requestedRange.end())) {
                break;
            }

            if (loadedRange.start().isAfter(cursor)) {
                missingRanges.add(
                        new TimeRange(
                                cursor,
                                minInstant(
                                        loadedRange.start(),
                                        requestedRange.end()
                                )
                        )
                );
            }

            if (loadedRange.end().isAfter(cursor)) {
                cursor = loadedRange.end();
            }

            if (!cursor.isBefore(requestedRange.end())) {
                return missingRanges;
            }
        }

        if (cursor.isBefore(requestedRange.end())
                || cursor.equals(requestedRange.start())) {
            missingRanges.add(
                    new TimeRange(
                            cursor,
                            requestedRange.end()
                    )
            );
        }

        return missingRanges;
    }

    private List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (ranges.isEmpty()) {
            return new ArrayList<>();
        }

        List<TimeRange> sortedRanges = new ArrayList<>(ranges);
        sortedRanges.sort(TimeRange::compareByStart);

        List<TimeRange> merged = new ArrayList<>();
        TimeRange current = sortedRanges.get(0);

        for (int i = 1; i < sortedRanges.size(); i++) {
            TimeRange next = sortedRanges.get(i);

            if (current.overlapsOrTouches(next)) {
                current = current.merge(next);
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);

        return merged;
    }

    private void validateRangeRequest(
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime
    ) throws DataCacheException {

        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException("Ticker cannot be empty.");
        }

        if (interval == null) {
            throw new DataCacheException("Interval cannot be null.");
        }

        if (startTime == null) {
            throw new DataCacheException("Start time cannot be null.");
        }

        if (endTime == null) {
            throw new DataCacheException("End time cannot be null.");
        }

        if (startTime.isAfter(endTime)) {
            throw new DataCacheException(
                    "Start time cannot be after end time."
            );
        }
    }

    private static Instant minInstant(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public int getTotalRows() {
        return cache.values()
                .stream()
                .mapToInt(entry -> entry.rows.size())
                .sum();
    }

    private static class CacheEntry {

        private final NavigableMap<Instant, StampedOHLCV> rows =
                new TreeMap<>();

        private List<TimeRange> loadedRanges =
                new ArrayList<>();
    }

    private record TimeRange(
            Instant start,
            Instant end
    ) {

        private static int compareByStart(
                TimeRange first,
                TimeRange second
        ) {
            return first.start.compareTo(second.start);
        }

        private boolean overlapsOrTouches(TimeRange other) {
            return !this.end.isBefore(other.start)
                    && !other.end.isBefore(this.start);
        }

        private TimeRange merge(TimeRange other) {
            return new TimeRange(
                    minInstant(this.start, other.start),
                    maxInstant(this.end, other.end)
            );
        }

        private static Instant maxInstant(Instant a, Instant b) {
            return a.isAfter(b) ? a : b;
        }
    }
}