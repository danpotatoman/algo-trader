package com.algotrader.marketdata.provider;

import java.time.Instant;
import java.util.List;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Primary abstraction for retrieving historical market data.
 *
 * <p>A {@code MarketDataProvider} supplies ordered OHLCV candlestick data
 * for a specific ticker, interval, and time range.
 *
 * <p>Implementations may retrieve data from:
 * <ul>
 *     <li>In-memory caches</li>
 *     <li>Databases</li>
 *     <li>CSV or other file-based sources</li>
 *     <li>Remote market data APIs</li>
 *     <li>Hybrid combinations of the above</li>
 * </ul>
 *
 * <p>This interface serves as the boundary between trading components and
 * the underlying market data infrastructure.
 *
 * <p>This interface is intentionally limited to market data retrieval. It
 * does not provide:
 * <ul>
 *     <li>Batching or sliding-window traversal</li>
 *     <li>Prediction generation</li>
 *     <li>Trading strategy logic</li>
 *     <li>Trade execution</li>
 * </ul>
 */
public interface MarketDataProvider {

    /**
     * Retrieves all available OHLCV rows within the requested time range.
     *
     * <p>The start and end timestamps are inclusive.
     *
     * <p>If no matching data exists, an empty list is returned.
     *
     * <p>The returned rows are ordered by ascending timestamp.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param startTime inclusive range start
     * @param endTime inclusive range end
     * @return ordered OHLCV rows within the requested range
     * @throws DataCacheException if the request cannot be satisfied
     */
    List<StampedOHLCV> requestRange(
            String ticker,
            TimeInterval interval,
            Instant startTime,
            Instant endTime
    ) throws DataCacheException;

    /**
     * Retrieves a single OHLCV row at an exact timestamp.
     *
     * <p>The requested timestamp must exactly match the timestamp of an
     * available candle. No interpolation, nearest-neighbor lookup, or interval
     * conversion is performed.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param timestamp exact candle timestamp
     * @return matching OHLCV row
     * @throws DataCacheException if the row cannot be retrieved or no matching
     *         candle exists
     */
    StampedOHLCV requestRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException;
}