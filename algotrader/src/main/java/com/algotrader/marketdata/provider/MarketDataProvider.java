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
     * <p>The first and last candle timestamps are inclusive.
     *
     * <p>If no matching data exists, an empty list is returned.
     *
     * <p>The returned rows are ordered by ascending timestamp.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param firstCandleTimestamp inclusive first candle timestamp
     * @param lastCandleTimestamp inclusive last candle timestamp
     * @return ordered OHLCV rows within the requested range
     * @throws DataCacheException if the request cannot be satisfied
     */
    List<StampedOHLCV> requestRange(
            String ticker,
            TimeInterval interval,
            Instant firstCandleTimestamp,
            Instant lastCandleTimestamp
    ) throws DataCacheException;

    /**
     * Retrieves a single OHLCV row at an exact candle timestamp.
     *
     * <p>The requested timestamp must exactly match the timestamp of an
     * available candle. No interpolation, nearest-neighbor lookup, or interval
     * conversion is performed.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param candleTimestamp exact candle timestamp
     * @return matching OHLCV row
     * @throws DataCacheException if the row cannot be retrieved or no matching
     *         candle exists
     */
    StampedOHLCV requestRow(
            String ticker,
            TimeInterval interval,
            Instant candleTimestamp
    ) throws DataCacheException;

    /**
     * Preloads market data for the specified tickers and time range.
     *
     * @param tickers the ticker symbols whose data should be loaded
     * @param interval the candle interval to preload
     * @param startInclusive the first candle timestamp to preload
     * @param endInclusive the last candle timestamp to preload
     * @throws DataCacheException if the requested data could not be loaded
     */
    default void preloadSession(
                List<String> tickers,
                TimeInterval interval,
                Instant startInclusive,
                Instant endInclusive
        ) throws DataCacheException {
        // Optional optimization. Implementations may override.
        }
}