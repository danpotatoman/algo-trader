package com.algotrader.marketdata.model;

import java.time.Instant;

/**
 * Represents a single OHLCV candlestick enriched with identifying metadata.
 *
 * <p>A {@code StampedOHLCV} combines raw {@link OHLCV} market data with:
 * <ul>
 *     <li>a ticker symbol</li>
 *     <li>a time interval</li>
 *     <li>a timestamp (typically the closing time of the candle)</li>
 * </ul>
 *
 * <p>Ticker symbols are normalized to uppercase during construction.
 *
 * <p>This record serves as the fundamental market-data unit within the
 * trading system and is used throughout storage, caching, batching
 * ({@link DataBatch}), and model-input preparation.
 * 
 * @param ticker the stock ticker symbol (e.g. {@code "AAPL"})
 * @param interval the candlestick time interval
 * @param timestamp the timestamp associated with the candle (typically the close time)
 * @param ohlcv the OHLCV data for the interval
 *
 * @throws IllegalArgumentException if:
 * <ul>
 *     <li>{@code ticker} is null or blank</li>
 *     <li>{@code interval} is null</li>
 *     <li>{@code timestamp} is null</li>
 *     <li>{@code ohlcv} is null</li>
 * </ul>
 */
public record StampedOHLCV(
    String ticker,
    TimeInterval interval,
    Instant timestamp,
    OHLCV ohlcv
) {

    /**
     * Validates the record contents and normalizes the ticker symbol.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public StampedOHLCV {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }
        if (ohlcv == null) {
            throw new IllegalArgumentException("OHLCV cannot be null.");
        }
        ticker = ticker.toUpperCase();
    }

    public double open() {
        return ohlcv.open();
    }

    public double high() {
        return ohlcv.high();
    }

    public double low() {
        return ohlcv.low();
    }

    public double close() {
        return ohlcv.close();
    }

    public long volume() {
        return ohlcv.volume();
    }

    /**
     * Returns a compact single-line representation of the candle suitable for
     * debugging and logging.
     *
     * @return formatted candle representation
     */
    @Override
    public String toString() {
        return ticker + " " + interval + " " + timestamp +
            " [o=" + open() +
            ", h=" + high() +
            ", l=" + low() +
            ", c=" + close() +
            ", v=" + volume() + "]";
    }
}