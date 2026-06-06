package com.algotrader.marketdata.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Provides sequential sliding windows over a loaded range of OHLCV data.
 *
 * <p>A {@code SlidingWindowProvider} requests an ordered range of market data
 * from a {@link MarketDataProvider}, stores that range in memory, and then
 * exposes overlapping {@link DataBatch} windows of a fixed size.
 *
 * <p>Each call to {@link #nextWindow()} advances the window by one row. For
 * example, with a batch size of 3, rows {@code [0,1,2]} are returned first,
 * followed by {@code [1,2,3]}, then {@code [2,3,4]}, and so on.
 *
 * <p>This class must be initialized by calling {@link #initialize()} before
 * windows can be requested. Initialization is separated from construction so
 * that data loading failures can be handled explicitly by callers.
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

    /**
     * Creates a sliding window provider.
     *
     * @param marketDataProvider provider used to load the source OHLCV range
     * @param ticker ticker symbol to load
     * @param interval candle interval for the requested data
     * @param batchSize number of rows in each generated window
     * @param startTime inclusive start of the data range
     * @param endTime inclusive end of the data range
     * @throws IllegalArgumentException if any argument is invalid
     */
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
     * <p>Each returned window contains {@code batchSize} rows. Consecutive calls
     * advance the window by one row.
     *
     * @return the next data batch, or {@link Optional#empty()} if no full window
     *         remains
     * @throws IllegalStateException if this provider has not been initialized
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
     * Initializes this provider by loading the configured market data range.
     *
     * <p>This method must be called before {@link #nextWindow()}. Repeated calls
     * after successful initialization have no effect.
     *
     * @throws DataCacheException if the configured data range cannot be loaded
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

    /**
     * Returns whether this provider has successfully loaded its source data.
     *
     * @return {@code true} if initialized; {@code false} otherwise
     */
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