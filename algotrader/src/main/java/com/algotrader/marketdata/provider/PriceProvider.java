package com.algotrader.marketdata.provider;

import java.time.Instant;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.MarketPrice;

/**
 * Provides market price lookup functionality.
 *
 * <p>A {@code PriceProvider} supplies a {@link MarketPrice} for a ticker at
 * a specific execution time.
 *
 * <p>The exact source of the price is implementation-dependent. Prices may
 * be derived from cached OHLCV data, databases, historical datasets, live
 * market feeds, or other data sources.
 *
 * <p>This interface exists as a higher-level abstraction than
 * {@link MarketDataProvider} for components that require only price lookup
 * functionality rather than access to full OHLCV candles.
 *
 * <p><b>Current design:</b> Lookups are performed using exact execution
 * times. A missing price is treated as an error condition and results in a
 * {@link DataCacheException}.
 *
 * <p><b>TODO:</b> Re-evaluate whether missing prices should continue to be
 * represented by exceptions or through a dedicated existence-check API.
 */
public interface PriceProvider {
    /**
     * Returns the market price for a ticker at a specific execution time.
     *
     * <p>The requested execution time must exactly match an available market
     * data point. No interpolation, nearest-neighbor lookup, or timestamp
     * rounding is performed.
     *
     * @param ticker ticker symbol to query
     * @param executionTime planned execution time
     * @return market price information
     * @throws DataCacheException if the price cannot be retrieved or no
     *         matching price exists
     */
    MarketPrice getTickerPrice(
            String ticker,
            Instant executionTime
    ) throws DataCacheException;
}