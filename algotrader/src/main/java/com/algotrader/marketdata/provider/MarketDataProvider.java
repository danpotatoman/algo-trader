package com.algotrader.marketdata.provider;

import java.time.Instant;
import java.util.List;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Provides ordered OHLCV market data retrieval.
 *
 * <p>Implementations may retrieve data from in-memory caches,
 * databases, files, APIs, or combinations thereof.
 *
 * <p>This interface does not provide batching, sliding-window
 * traversal, prediction logic, or trading functionality.
 */
public interface MarketDataProvider {

    /**
     * Retrieves all available OHLCV rows for the specified ticker,
     * interval, and time range.
     *
     * <p>The start and end timestamps are inclusive.
     *
     * <p>If no data exists within the requested range, an empty list
     * is returned.
     *
     * <p>The returned rows are ordered by timestamp ascending.
     *
     * @param ticker the stock ticker symbol, e.g. {@code "AAPL"}
     * @param interval the candlestick interval
     * @param startTime the beginning of the requested range (inclusive)
     * @param endTime the end of the requested range (inclusive)
     * @return an ordered list of matching OHLCV rows
     * @throws DataCacheException if the data cannot be retrieved
     */
    List<StampedOHLCV> requestRange(
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime
    ) throws DataCacheException;

    /**
     * Retrieves the OHLCV row for the specified ticker, interval,
     * and timestamp.
     *
     * @param ticker the stock ticker symbol, e.g. {@code "AAPL"}
     * @param interval the candlestick interval
     * @param timestamp the timestamp of the requested candle
     * @return the matching OHLCV row
     * @throws DataCacheException if the data cannot be retrieved
     *         or no matching candle exists
     */
    StampedOHLCV requestRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException;
}