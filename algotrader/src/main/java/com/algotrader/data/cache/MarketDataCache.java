package com.algotrader.data.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.MarketDataKey;
import com.algotrader.data.dataobjects.MarketPrice;
import com.algotrader.data.dataobjects.StampedOHLCV;
import com.algotrader.data.source.OHLCVSource;

/**
 * Caches OHLCV market data loaded from one or more {@link OHLCVSource}s.
 *
 * <p>This class owns caching, timestamp navigation, and batch construction.
 * It does not know how individual sources retrieve their data.
 */
public class MarketDataCache implements MarketDataProvider {

    private final List<OHLCVSource> sources;

    private final Map<String,
            Map<TimeInterval,
                    NavigableMap<Instant, StampedOHLCV>>> rows = new HashMap<>();

    /**
     * Tracks ticker/interval pairs for which all file-based sources
     * have already been checked.
     */
    private final Set<MarketDataKey> checkedFileBasedKeys = new HashSet<>();

    public MarketDataCache(List<OHLCVSource> sources) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("Sources cannot be null or empty.");
        }

        if (sources.stream().anyMatch(source -> source == null)) {
            throw new IllegalArgumentException("Sources cannot contain null values.");
        }

        this.sources = List.copyOf(sources);
    }

    @Override
    public DataBatch requestBatch(
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant closingTimestamp
    ) throws DataCacheException {

        validateRequest(ticker, interval, batchSize);

        if (closingTimestamp == null) {
            throw new DataCacheException("Closing timestamp cannot be null.");
        }

        String normalizedTicker = ticker.toUpperCase();

        ensureRowsLoaded(normalizedTicker, interval);

        NavigableMap<Instant, StampedOHLCV> matchingRows =
                getRowsFor(normalizedTicker, interval);

        if (!matchingRows.containsKey(closingTimestamp)) {
            throw new DataCacheException(
                    "No candle found for ticker " + normalizedTicker
                            + ", interval " + interval
                            + ", timestamp " + closingTimestamp
            );
        }

        NavigableMap<Instant, StampedOHLCV> rowsUpToClosingTimestamp =
                matchingRows.headMap(closingTimestamp, true);

        List<StampedOHLCV> batchRows = new ArrayList<>(
                rowsUpToClosingTimestamp
                        .descendingMap()
                        .values()
                        .stream()
                        .limit(batchSize)
                        .toList()
        );

        if (batchRows.size() < batchSize) {
            throw new DataCacheException(
                    "Not enough data before timestamp " + closingTimestamp
                            + " to build batch of size " + batchSize
            );
        }

        Collections.reverse(batchRows);

        return new DataBatch(interval, batchRows);
    }

    @Override
    public Instant getNextTimestamp(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException {

        if (timestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }

        validateRequest(ticker, interval, 1);

        String normalizedTicker = ticker.toUpperCase();

        ensureRowsLoaded(normalizedTicker, interval);

        NavigableMap<Instant, StampedOHLCV> rowsByTimestamp =
                getRowsFor(normalizedTicker, interval);

        Map.Entry<Instant, StampedOHLCV> nextEntry =
                rowsByTimestamp.higherEntry(timestamp);

        if (nextEntry == null) {
            throw new DataCacheException(
                    "No timestamp exists after " + timestamp
                            + " for ticker " + normalizedTicker
                            + " and interval " + interval
            );
        }

        return nextEntry.getKey();
    }

    public MarketPrice getTickerPrice(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        try {
            validateRequest(ticker, interval, 1);

            String normalizedTicker = ticker.toUpperCase();

            ensureRowsLoaded(normalizedTicker, interval);

            NavigableMap<Instant, StampedOHLCV> rowsByTimestamp =
                    getRowsFor(normalizedTicker, interval);

            StampedOHLCV row = rowsByTimestamp.get(timestamp);

            if (row == null) {
                throw new IllegalArgumentException(
                        "No market price found for ticker "
                                + normalizedTicker
                                + ", interval "
                                + interval
                                + ", timestamp "
                                + timestamp
                );
            }

            return new MarketPrice(
                    normalizedTicker,
                    row.close(),
                    row.timestamp()
            );

        } catch (DataCacheException e) {
            throw new IllegalArgumentException(
                    "Could not get market price for ticker "
                            + ticker
                            + ", interval "
                            + interval
                            + ", timestamp "
                            + timestamp,
                    e
            );
        }
    }

    private void ensureRowsLoaded(
            String ticker,
            TimeInterval interval
    ) throws DataCacheException {

        MarketDataKey key = new MarketDataKey(ticker, interval);
        boolean fileSourcesAlreadyChecked = checkedFileBasedKeys.contains(key);

        for (OHLCVSource source : sources) {
            if (fileSourcesAlreadyChecked && source.isFileBased()) {
                continue;
            }

            List<StampedOHLCV> loadedRows = source.loadRows(ticker, interval);

            if (loadedRows == null || loadedRows.isEmpty()) {
                continue;
            }

            mergeRows(ticker, interval, loadedRows);
        }

        if (!fileSourcesAlreadyChecked) {
            checkedFileBasedKeys.add(key);
        }
    }

    private void mergeRows(
            String ticker,
            TimeInterval interval,
            List<StampedOHLCV> loadedRows
    ) {
        NavigableMap<Instant, StampedOHLCV> rowsByTimestamp = rows
                .computeIfAbsent(ticker, ignored -> new HashMap<>())
                .computeIfAbsent(interval, ignored -> new TreeMap<>());

        for (StampedOHLCV row : loadedRows) {
            rowsByTimestamp.put(row.timestamp(), row);
        }
    }

    private NavigableMap<Instant, StampedOHLCV> getRowsFor(
            String ticker,
            TimeInterval interval
    ) {
        Map<TimeInterval, NavigableMap<Instant, StampedOHLCV>> rowsByInterval =
                rows.get(ticker);

        if (rowsByInterval == null) {
            return new TreeMap<>();
        }

        NavigableMap<Instant, StampedOHLCV> rowsByTimestamp =
                rowsByInterval.get(interval);

        if (rowsByTimestamp == null) {
            return new TreeMap<>();
        }

        return rowsByTimestamp;
    }

    private void validateRequest(
            String ticker,
            TimeInterval interval,
            int batchSize
    ) throws DataCacheException {

        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException("Ticker cannot be empty.");
        }

        if (interval == null) {
            throw new DataCacheException("Interval cannot be null.");
        }

        if (batchSize <= 0) {
            throw new DataCacheException("Batch size must be positive.");
        }
    }

    public int getTotalRows() {
        return rows.values()
                .stream()
                .flatMap(intervalMap -> intervalMap.values().stream())
                .mapToInt(NavigableMap::size)
                .sum();
    }
}