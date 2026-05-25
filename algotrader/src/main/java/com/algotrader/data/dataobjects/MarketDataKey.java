package com.algotrader.data.dataobjects;

import com.algotrader.data.TimeInterval;

/**
 * Identifies a cached market dataset by ticker and interval.
 *
 * @param ticker the ticker symbol
 * @param interval the candlestick interval
 */
public record MarketDataKey(
        String ticker,
        TimeInterval interval
) {

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