package com.algotrader.data.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.StampedOHLCV;

/**
 * Provides sequential sliding windows over an ordered range of OHLCV data.
 *
 * <p>This class does not retrieve individual batches from the data provider.
 * Instead, it loads the requested range once during construction and then
 * iterates through that range in memory.
 */
public class SlidingWindowProvider {

    private final MarketDataProvider marketDataProvider;
    private final String ticker;
    private final TimeInterval interval;
    private final int batchSize;
    private final Instant startTime;
    private final Instant endTime;

    private List<StampedOHLCV> rows;
    private int nextEndIndex;
    private boolean initialized;


     public SlidingWindowProvider(
            MarketDataProvider marketDataProvider,
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant startTime,
            Instant endTime
    ) {
        validateConstructorArgs(marketDataProvider, ticker, interval, batchSize, startTime, endTime);
        
        this.marketDataProvider = marketDataProvider;
        this.ticker = ticker.toUpperCase();
        this.interval = interval;
        this.batchSize = batchSize;
        this.startTime = startTime;
        this.endTime = endTime;

        this.rows = List.of();
        this.nextEndIndex = batchSize - 1;
        this.initialized = false;
    }

    /**
     * Returns the next full sliding window if one exists.
     *
     * <p>Each returned window contains {@code batchSize} rows. Consecutive
     * calls advance the window by one row.
     *
     * @return the next {@link DataBatch}, or {@link Optional#empty()} if no
     *         full window remains
     */
    public Optional<DataBatch> nextWindow() {
        if (!initialized) {
            throw new IllegalStateException(
                    "SlidingWindowProvider must be initialized before nextWindow() is called."
            );
        }

        if (nextEndIndex >= rows.size()) {
            return Optional.empty();
        }

        int startIndex = nextEndIndex - batchSize + 1;

        List<StampedOHLCV> windowRows =
                new ArrayList<>(
                        rows.subList(startIndex, nextEndIndex + 1)
                );

        nextEndIndex++;

        return Optional.of(new DataBatch(interval, windowRows));
    }

    /**
     * Returns the number of full windows remaining, including the next one.
     *
     * @return number of remaining full windows
     */
    public int remainingWindows() {
        return Math.max(0, rows.size() - nextEndIndex);
    }

    /**
     * Returns the total number of rows loaded into this provider.
     *
     * @return loaded row count
     */
    public int loadedRowCount() {
        return rows.size();
    }

    /**
     * Initializes this SlidingWinodwProvider, preparing it for nextWindow() calls.
     * @throws DataCacheException
     */
    public void initialize() throws DataCacheException {
        if (initialized) {
            return;
        }

        this.rows = List.copyOf(
                marketDataProvider.requestRange(
                        ticker,
                        interval,
                        startTime,
                        endTime
                )
        );

        this.nextEndIndex = batchSize - 1;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void validateConstructorArgs(
            MarketDataProvider marketDataProvider,
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant startTime,
            Instant endTime
    ) {

        if (marketDataProvider == null) {
            throw new IllegalArgumentException(
                    "MarketDataProvider cannot be null."
            );
        }

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be null or blank."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "Interval cannot be null."
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be positive."
            );
        }

        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time cannot be null."
            );
        }

        if (endTime == null) {
            throw new IllegalArgumentException(
                    "End time cannot be null."
            );
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "Start time cannot be after end time."
            );
        }
    }
}