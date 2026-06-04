package com.algotrader.marketdata.model;

import java.time.Instant;

/**
 * Represents a single OHLCV candlestick enriched with identifying metadata.
 *
 * <p>A {@code StampedOHLCV} combines raw {@link OHLCV} price/volume data with:
 * <ul>
 *     <li>a ticker symbol</li>
 *     <li>a time interval</li>
 *     <li>a timestamp (typically the closing time of the candle)</li>
 * </ul>
 *
 * <p>This class is immutable and is commonly used as the fundamental data unit
 * within the data pipeline, including storage, batching ({@link DataBatch}),
 * and model input preparation.
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
     * Validates that all components of the record are present and valid.
     *
     * <p>Ensures that ticker, interval, timestamp, and OHLCV data are non-null
     * and that the ticker is non-empty.
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

    /**
     * Returns the opening price of the candle.
     *
     * @return the open price
     */
    public double open() {
        return ohlcv.open();
    }

    /**
     * Returns the highest price reached during the interval.
     *
     * @return the high price
     */
    public double high() {
        return ohlcv.high();
    }

    /**
     * Returns the lowest price reached during the interval.
     *
     * @return the low price
     */
    public double low() {
        return ohlcv.low();
    }

    /**
     * Returns the closing price of the candle.
     *
     * @return the close price
     */
    public double close() {
        return ohlcv.close();
    }

    /**
     * Returns the traded volume during the interval.
     *
     * @return the volume
     */
    public long volume() {
        return ohlcv.volume();
    }

    /**
     * Returns a compact string representation of this candlestick.
     *
     * <p>The format includes ticker, interval, timestamp, and OHLCV values
     * in a concise single-line form suitable for logging:
     *
     * <pre>
     * AAPL FIVE_MINUTES 2026-04-24T13:30:00Z [o=..., h=..., l=..., c=..., v=...]
     * </pre>
     *
     * @return a human-readable string representation of this object
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