package com.algotrader.data.cache;

import java.time.Instant;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.DataBatch;

/**
 * Provides sequential sliding-window access over a {@link MarketDataProvider}.
 *
 * <p>Each instance owns its own traversal state, so multiple
 * SlidingWindowProvider instances can share the same underlying MarketDataProvider
 * without interfering with each other.
 */
public class SlidingWindowProvider {

    private final String ticker;
    private final TimeInterval interval;
    private final int batchSize;
    private final MarketDataProvider marketDataProvider;

    private Instant currentTimestamp;

    public SlidingWindowProvider(
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant startingTime,
            MarketDataProvider marketDataProvider
    ) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be empty.");
        }

        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive.");
        }

        if (startingTime == null) {
            throw new IllegalArgumentException("Starting time cannot be null.");
        }

        if (marketDataProvider == null) {
            throw new IllegalArgumentException("MarketDataProvider cannot be null.");
        }

        this.ticker = ticker.toUpperCase();
        this.interval = interval;
        this.batchSize = batchSize;
        this.currentTimestamp = startingTime;
        this.marketDataProvider = marketDataProvider;
    }

    /**
     * Returns the next DataBatch in the sliding sequence.
     *
     * <p>The first call returns a batch ending at the starting timestamp
     * provided at construction. Each following call advances to the next
     * available timestamp in the underlying MarketDataProvider.
     *
     * @return the next sliding-window DataBatch
     * @throws DataCacheException if the next window cannot be produced
     */
    public DataBatch nextWindow() throws DataCacheException {
        DataBatch batch = marketDataProvider.requestBatch(
                ticker,
                interval,
                batchSize,
                currentTimestamp
        );

        currentTimestamp = marketDataProvider.getNextTimestamp(
                ticker,
                interval,
                currentTimestamp
        );

        return batch;
    }

    public String getTicker() {
        return ticker;
    }
}
