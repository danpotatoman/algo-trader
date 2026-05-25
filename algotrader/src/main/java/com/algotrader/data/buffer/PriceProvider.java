package com.algotrader.data.buffer;

import java.time.Instant;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.MarketPrice;

/**
 * Provides exact market price lookup functionality for financial instruments.
 *
 * <p>Implementations may retrieve prices from historical data, in-memory caches,
 * databases, broker APIs, real-time market feeds, or simulated trading
 * environments.
 *
 * <p>The requested timestamp is treated as an exact lookup time. Implementations
 * should throw if no price is available for that exact timestamp.
 */
public interface PriceProvider {

    /**
     * Retrieves market price information for a ticker at the specified timestamp.
     *
     * <p>The returned {@link MarketPrice} must correspond exactly to the requested
     * ticker and timestamp.
     *
     * <p>The returned price is the closing price of the candle on that interval.
     *
     * @param ticker the ticker symbol to query, e.g. {@code "AAPL"}
     * @param interval the OHLCV interval to query
     * @param timestamp the exact lookup timestamp
     * @return market price information for the exact requested timestamp
     * @throws IllegalArgumentException if ticker or timestamp is invalid, or if no
     *         price is available for the exact timestamp
     */
    MarketPrice getTickerPrice(
            String ticker,
            TimeInterval interval, //TODO: its goofy that PriceProvier needs an interval at all. This may need to change
            Instant timestamp
    );
}