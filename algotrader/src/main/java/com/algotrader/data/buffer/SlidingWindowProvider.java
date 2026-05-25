package com.algotrader.data.buffer;

import java.time.Instant;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.DataBatch;

/**
 * Provides sequential sliding-window access over a {@link DataBuffer}.
 *
 * <p>Each instance owns its own traversal state, so multiple
 * SlidingWindowProvider instances can share the same underlying DataBuffer
 * without interfering with each other.
 */
public class SlidingWindowProvider {

    private final String ticker;
    private final TimeInterval interval;
    private final int batchSize;
    private final DataBuffer dataBuffer;

    private Instant currentTimestamp;

    public SlidingWindowProvider(
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant startingTime,
            DataBuffer dataBuffer
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

        if (dataBuffer == null) {
            throw new IllegalArgumentException("DataBuffer cannot be null.");
        }

        this.ticker = ticker.toUpperCase();
        this.interval = interval;
        this.batchSize = batchSize;
        this.currentTimestamp = startingTime;
        this.dataBuffer = dataBuffer;
    }

    /**
     * Returns the next DataBatch in the sliding sequence.
     *
     * <p>The first call returns a batch ending at the starting timestamp
     * provided at construction. Each following call advances to the next
     * available timestamp in the underlying DataBuffer.
     *
     * @return the next sliding-window DataBatch
     * @throws DataBufferException if the next window cannot be produced
     */
    public DataBatch nextWindow() throws DataBufferException {
        DataBatch batch = dataBuffer.requestBatch(
                ticker,
                interval,
                batchSize,
                currentTimestamp
        );

        currentTimestamp = dataBuffer.getNextTimestamp(
                ticker,
                interval,
                currentTimestamp
        );

        return batch;
    }
}