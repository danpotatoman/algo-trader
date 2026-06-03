package com.algotrader.data.provider;

import java.time.Instant;

import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.dataobjects.MarketPrice;

/**
 * Returns the market price for the specified ticker and timestamp.
 *
 * <p>The timestamp is treated as an exact lookup. Implementations
 * must either return the matching market price or throw an exception.
 *
 * <p>A missing price is considered an error condition rather than
 * a normal result because callers are expected to request timestamps
 * known to exist.
 *
 * @param ticker the ticker symbol
 * @param timestamp the requested timestamp
 * @return market price information
 * @throws DataCacheException if the price cannot be retrieved or no
 *         price exists for the specified timestamp
 */
public interface PriceProvider {

    /**
     * Returns the market price for the specified ticker and timestamp.
     *
     * @param ticker the ticker symbol
     * @param timestamp the requested timestamp
     * @return market price information
     * @throws DataCacheException if the price cannot be retrieved
     */
    MarketPrice getTickerPrice(
            String ticker,
            Instant timestamp
    ) throws DataCacheException;
}