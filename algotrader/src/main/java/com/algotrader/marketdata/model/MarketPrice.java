package com.algotrader.marketdata.model;

import java.time.Instant;

/**
 * Represents a market price associated with a ticker and executionTime.
 *
 * <p>A {@code MarketPrice} is a lightweight value object used throughout the
 * trading system when only a price lookup result is needed rather than a full
 * OHLCV candle.
 *
 * <p>The meaning of the price depends on the component that produced it. For
 * example, the current {@code MarketDataCache} implementation derives market
 * prices from candle close values.
 *
 * <p>Ticker symbols are normalized to uppercase during construction.
 *
 * @param ticker ticker symbol associated with the price
 * @param price market price value
 * @param executionTime executionTime associated with the price
 */
public record MarketPrice(
        String ticker,
        double price,
        Instant executionTime
) {

    /**
     * Creates a market price.
     *
     * @throws IllegalArgumentException if the ticker is null or blank, if the
     *         executionTime is null, price is NaN, or price is negative
     */
    public MarketPrice {

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be empty."
            );
        }

        if (executionTime == null) {
            throw new IllegalArgumentException(
                    "Execution time cannot be null."
            );
        }

        if (Double.isNaN(price)) {
            throw new IllegalArgumentException(
                    "Price cannot be NaN."
            );
        }

        if (price < 0.0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative."
            );
        }

        ticker = ticker.toUpperCase();
    }
}