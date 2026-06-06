package com.algotrader.marketdata.model;

/**
 * Immutable representation of a single OHLCV candlestick.
 *
 * <p>An {@code OHLCV} record captures the open, high, low, close, and volume
 * values associated with a single market-data interval.
 *
 * <p>This record enforces basic financial data invariants:
 * <ul>
 *     <li>{@code high >= low}</li>
 *     <li>{@code open} and {@code close} must lie within [{@code low}, {@code high}]</li>
 *     <li>{@code volume >= 0}</li>
 * </ul>
 *
 * <p>These constraints ensure that the candlestick represents valid market data.
 *
 * <p>This class is immutable and is typically used as a building block for
 * higher-level data structures such as {@link StampedOHLCV} and {@link DataBatch}.
 *
 * @param open the opening price of the interval
 * @param high the highest price reached during the interval
 * @param low the lowest price reached during the interval
 * @param close the closing price of the interval
 * @param volume the total traded volume during the interval
 *
 * @throws IllegalArgumentException if any of the following conditions are violated:
 * <ul>
 *     <li>{@code high < low}</li>
 *     <li>{@code open} is not within [{@code low}, {@code high}]</li>
 *     <li>{@code close} is not within [{@code low}, {@code high}]</li>
 *     <li>{@code volume < 0}</li>
 * </ul>
 */
public record OHLCV(
    double open,
    double high,
    double low,
    double close,
    long volume
) {

    /**
     * Validates that the OHLCV values form a consistent candlestick.
     *
     * @throws IllegalArgumentException if any financial data invariant is
     *         violated
     */
    public OHLCV { //TODO: include validation for NaN and posInfinity/negInfinity
        if (high < low) {
            throw new IllegalArgumentException("High cannot be less than low.");
        }
        if (open < low || open > high) {
            throw new IllegalArgumentException("Open must be between low and high.");
        }
        if (close < low || close > high) {
            throw new IllegalArgumentException("Close must be between low and high.");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("Volume cannot be negative.");
        }
    }
}