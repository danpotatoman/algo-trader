package com.algotrader.marketdata.model;

import java.time.Instant;

/**
 * Represents a market price for a specific ticker at a specific timestamp.
 *
 * @param ticker the ticker symbol
 * @param price the market price
 * @param timestamp the timestamp associated with the price
 */
public record MarketPrice(
        String ticker,
        double price,
        Instant timestamp
) {

    public MarketPrice {

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be empty."
            );
        }

        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
        }

        ticker = ticker.toUpperCase();
    }
}