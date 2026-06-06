package com.algotrader.marketdata.model;

/**
 * Unique identifier for a cached market dataset.
 *
 * <p>A {@code MarketDataKey} combines a ticker symbol and
 * {@link TimeInterval} to identify a specific collection of OHLCV data
 * within the market data cache.
 *
 * <p>For example:
 *
 * <pre>
 * AAPL + FIVE_MINUTES
 * MSFT + ONE_HOUR
 * </pre>
 *
 * <p>Ticker symbols are normalized to uppercase during construction.
 *
 * @param ticker ticker symbol associated with the dataset
 * @param interval candle interval associated with the dataset
 */
public record MarketDataKey(
        String ticker,
        TimeInterval interval
) {

    /**
     * Creates a market data key.
     *
     * @throws IllegalArgumentException if the ticker is null or blank, or if
     *         the interval is null
     */
    public MarketDataKey {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be empty."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "Interval cannot be null."
            );
        }

        ticker = ticker.toUpperCase();
    }
}