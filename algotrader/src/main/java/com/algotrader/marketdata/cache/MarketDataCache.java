package com.algotrader.marketdata.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.algotrader.marketdata.model.MarketDataKey;
import com.algotrader.marketdata.model.MarketPrice;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.marketdata.provider.MarketDataProvider;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.marketdata.source.OHLCVSource;

/**
 * Caching implementation of {@link MarketDataProvider} and
 * {@link PriceProvider}.
 *
 * <p>
 * {@code MarketDataCache} provides efficient access to historical market
 * data by caching rows loaded from an underlying {@link OHLCVSource}. Loaded
 * data is retained in memory and reused across future requests, reducing
 * redundant source queries.
 *
 * <p>
 * The cache tracks which time ranges have already been loaded for each
 * ticker and interval combination. Requests for previously loaded ranges are
 * served entirely from memory, while requests for new ranges trigger
 * incremental loading from the underlying source.
 *
 * <p>
 * This class serves as the primary access layer between trading
 * components and market data sources.
 *
 * <p>
 * Responsibilities include:
 * <ul>
 * <li>Range-based OHLCV retrieval</li>
 * <li>Single-row OHLCV retrieval</li>
 * <li>Market price lookup by timestamp</li>
 * <li>In-memory caching of loaded market data</li>
 * <li>Tracking which time ranges have already been loaded</li>
 * </ul>
 *
 * <p>
 * This class does not construct batches, perform sliding-window
 * traversal, schedule trading cycles, or manage execution logic.
 */
public class MarketDataCache
        implements MarketDataProvider, PriceProvider {

    private final OHLCVSource source;

    private final Map<MarketDataKey, CacheEntry> cache = new HashMap<>();

    private static final List<TimeInterval> PRICE_LOOKUP_INTERVALS = List.of(
            TimeInterval.ONE_MINUTE,
            TimeInterval.FIVE_MINUTES,
            TimeInterval.FIFTEEN_MINUTES,
            TimeInterval.THIRTY_MINUTES,
            TimeInterval.ONE_HOUR,
            TimeInterval.ONE_DAY);

    /**
     * Creates a market data cache backed by the supplied data source.
     *
     * @param source underlying source used to load OHLCV data
     * @throws IllegalArgumentException if {@code source} is null
     */
    public MarketDataCache(OHLCVSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null.");
        }

        this.source = source;
    }

    /**
     * Returns all OHLCV rows within the requested time range.
     *
     * <p>
     * Any portions of the requested range not already present in the cache
     * are loaded from the underlying source before the result is returned.
     *
     * @param ticker    ticker symbol to query
     * @param interval  candle interval to query
     * @param startTime inclusive range start
     * @param endTime   inclusive range end
     * @return OHLCV rows within the requested range
     * @throws DataCacheException if the request is invalid or data cannot be
     *                            loaded
     */
    @Override
    public List<StampedOHLCV> requestRange(
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime) throws DataCacheException {

        validateRangeRequest(ticker, interval, startTime, endTime);

        String normalizedTicker = ticker.toUpperCase();
        MarketDataKey key = new MarketDataKey(normalizedTicker, interval);

        CacheEntry entry = cache.computeIfAbsent(
                key,
                ignored -> new CacheEntry());

        ensureRangeLoaded(
                entry,
                normalizedTicker,
                interval,
                startTime,
                endTime);

        return new ArrayList<>(
                entry.rows
                        .subMap(startTime, true, endTime, true)
                        .values());
    }

    /**
     * Returns the OHLCV row for an exact timestamp.
     *
     * <p>
     * This method delegates to {@link #requestRange(String, TimeInterval,
     * Instant, Instant)} and therefore benefits from the same caching behavior.
     *
     * @param ticker    ticker symbol to query
     * @param interval  candle interval to query
     * @param timestamp timestamp to retrieve
     * @return matching OHLCV row
     * @throws DataCacheException if the row does not exist or cannot be loaded
     */
    @Override
    public StampedOHLCV requestRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp) throws DataCacheException {

        if (timestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }

        List<StampedOHLCV> rows = requestRange(
                ticker,
                interval,
                timestamp,
                timestamp);

        if (rows.isEmpty()) {
            throw new DataCacheException(
                    "No candle found for ticker "
                            + ticker.toUpperCase()
                            + ", interval "
                            + interval
                            + ", timestamp "
                            + timestamp);
        }

        return rows.get(0);
    }

    /**
     * Returns a market price for the specified ticker and timestamp.
     *
     * <p>
     * The cache searches all supported intervals for a candle whose timestamp
     * exactly matches the requested timestamp. If a matching candle is found,
     * its close price is returned as a {@link MarketPrice}.
     *
     * <p>
     * The lookup first searches already-cached data and then attempts to load
     * missing data from the underlying source if necessary.
     *
     * <p>
     * <b>Note:</b> This method currently requires an exact timestamp match.
     * No interpolation, nearest-neighbor lookup, or interval conversion is
     * performed.
     *
     * @param ticker    ticker symbol to query
     * @param timestamp timestamp of the desired price
     * @return market price at the requested timestamp
     * @throws DataCacheException if no matching price can be found
     */
    @Override
    public MarketPrice getTickerPrice(
            String ticker,
            Instant timestamp) throws DataCacheException {
        /**
         * TODO: Re-evaluate whether missing prices should be represented by
         * DataCacheException or through a dedicated existence-check API.
         */
        validatePriceRequest(ticker, timestamp);

        String normalizedTicker = ticker.toUpperCase();

        /*
         * First pass: check already-cached rows directly.
         */
        for (TimeInterval interval : PRICE_LOOKUP_INTERVALS) {
            MarketDataKey key = new MarketDataKey(
                    normalizedTicker,
                    interval);

            CacheEntry entry = cache.get(key);

            if (entry == null) {
                continue;
            }

            StampedOHLCV row = entry.rows.get(timestamp);

            if (row != null) {
                return new MarketPrice(
                        normalizedTicker,
                        row.open(),
                        timestamp);
            }
        }

        /*
         * Second pass: ask the source/cache system to load rows if needed.
         */
        for (TimeInterval interval : PRICE_LOOKUP_INTERVALS) {
            try {
                StampedOHLCV row = requestRow(
                        normalizedTicker,
                        interval,
                        timestamp);

                return new MarketPrice(
                        normalizedTicker,
                        row.open(),
                        timestamp);

            } catch (DataCacheException ignored) {
                /*
                 * Missing data at one interval does not necessarily mean missing
                 * data at all intervals.
                 */
            }
        }

        throw new DataCacheException(
                "No market price found for ticker "
                        + normalizedTicker
                        + " at timestamp "
                        + timestamp);
    }

    @Override
    public void preloadSession(
            List<String> tickers,
            TimeInterval interval,
            Instant startInclusive,
            Instant endInclusive
    ) throws DataCacheException {

        for (String ticker : tickers) {
            requestRange(
                    ticker,
                    interval,
                    startInclusive,
                    endInclusive
            );
        }
    }

    private void validatePriceRequest(
            String ticker,
            Instant timestamp) throws DataCacheException {

        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException("Ticker cannot be empty.");
        }

        if (timestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }
    }

    /**
     * Ensures that all data within the requested range has been loaded into the
     * cache.
     *
     * <p>
     * Only portions of the range not already covered by previously loaded
     * ranges are requested from the underlying source.
     *
     * @param entry     cache entry to populate
     * @param ticker    ticker symbol
     * @param interval  candle interval
     * @param startTime inclusive range start
     * @param endTime   inclusive range end
     * @throws DataCacheException if data loading fails
     */
    private void ensureRangeLoaded(
            CacheEntry entry,
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime) throws DataCacheException {

        List<TimeRange> missingRanges = findMissingRanges(
                entry.loadedRanges,
                new TimeRange(startTime, endTime));

        for (TimeRange missingRange : missingRanges) {
            List<StampedOHLCV> loadedRows = source.loadRange(
                    ticker,
                    interval,
                    missingRange.start(),
                    missingRange.end());

            if (loadedRows != null) {
                for (StampedOHLCV row : loadedRows) {
                    entry.rows.put(row.candleOpenTime(), row);
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

    /**
     * Determines which portions of a requested range have not yet been loaded.
     *
     * <p>The returned ranges represent gaps between the requested range and the
     * set of already-loaded ranges. These gaps are the only portions that need
     * to be queried from the underlying data source.
     *
     * @param loadedRanges ranges already known to be loaded
     * @param requestedRange range being requested
     * @return missing subranges that must still be loaded
     */
    private List<TimeRange> findMissingRanges(
            List<TimeRange> loadedRanges,
            TimeRange requestedRange) {
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
                                        requestedRange.end())));
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
                            requestedRange.end()));
        }

        return missingRanges;
    }

    /**
     * Merges overlapping or adjacent time ranges into the smallest possible set
     * of non-overlapping ranges.
     *
     * <p>This method is used to maintain a compact representation of loaded
     * regions within the cache.
     *
     * @param ranges ranges to merge
     * @return merged non-overlapping ranges ordered by start time
     */
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
            Instant endTime) throws DataCacheException {

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
                    "Start time cannot be after end time.");
        }
    }

    private static Instant minInstant(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    /**
     * Returns the total number of cached OHLCV rows across all tickers and
     * intervals.
     *
     * @return total cached row count
     */
    public int getTotalRows() {
        return cache.values()
                .stream()
                .mapToInt(entry -> entry.rows.size())
                .sum();
    }

    /**
     * Internal cache state for a single ticker and interval combination.
     *
     * <p>
     * Stores both loaded OHLCV rows and the set of time ranges that have
     * already been queried from the underlying source.
     */
    private static class CacheEntry {

        private final NavigableMap<Instant, StampedOHLCV> rows = new TreeMap<>();

        private List<TimeRange> loadedRanges = new ArrayList<>();
    }

    /**
     * Immutable representation of a loaded time range.
     *
     * <p>
     * Used internally to track which portions of a ticker/interval dataset
     * have already been requested from the underlying source.
     */
    private record TimeRange(
            Instant start,
            Instant end) {

        private static int compareByStart(
                TimeRange first,
                TimeRange second) {
            return first.start.compareTo(second.start);
        }

        /**
         * Returns whether two ranges overlap or directly touch.
         *
         * <p>Ranges that touch are considered mergeable.
         */
        private boolean overlapsOrTouches(TimeRange other) {
            return !this.end.isBefore(other.start)
                    && !other.end.isBefore(this.start);
        }

        /**
         * Returns a range spanning both input ranges.
         *
         * <p>The ranges are assumed to overlap or touch.
         */
        private TimeRange merge(TimeRange other) {
            return new TimeRange(
                    minInstant(this.start, other.start),
                    maxInstant(this.end, other.end));
        }

        private static Instant maxInstant(Instant a, Instant b) {
            return a.isAfter(b) ? a : b;
        }
    }
}