package com.algotrader.data.cache;

import java.time.Instant;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.DataBatch;

public interface MarketDataProvider {

    /**
     * Returns a DataBatch containing OHLCV data ending at a specific timestamp.
     *
     * <p>The returned batch should contain {@code batchSize} rows ending at
     * {@code closingTimestamp}, inclusive.
     *
     * <p>Implementations may fetch from an external API, use cached data,
     * load from a file, or combine multiple sources.
     *
     * @param ticker the stock symbol, e.g. {@code "AAPL"}
     * @param interval the time interval between candles/data points
     * @param batchSize the size of the batch, in number of candles
     * @param closingTimestamp the timestamp of the final candle in the batch
     * @return a DataBatch containing the requested data
     * @throws DataBufferException if the batch cannot be produced
     */
    DataBatch requestBatch(
            String ticker,
            TimeInterval interval,
            int batchSize,
            Instant closingTimestamp
    ) throws DataCacheException;

    /**
     * Returns the next available timestamp after the provided timestamp
     * for the given ticker and interval.
     *
     * <p>The returned timestamp must correspond to an actual available
     * candle/data point in the buffer.
     *
     * <p>This is useful for sequential traversal of historical data,
     * such as sliding-window backtesting or simulation.
     *
     * @param ticker the stock symbol, e.g. {@code "AAPL"}
     * @param interval the time interval between candles/data points
     * @param timestamp the reference timestamp
     * @return the next available timestamp after {@code timestamp}
     * @throws DataCacheException if no later timestamp exists or the request is invalid
     */
    Instant getNextTimestamp(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException;
}