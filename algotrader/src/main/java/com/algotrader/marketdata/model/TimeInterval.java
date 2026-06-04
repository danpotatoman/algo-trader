package com.algotrader.marketdata.model;

/**
 * Represents supported time intervals for OHLCV candlestick data.
 *
 * <p>Each interval defines:
 * <ul>
 *     <li>A resolution string used by external APIs (e.g. Finnhub)</li>
 *     <li>The duration of the interval in seconds</li>
 * </ul>
 *
 * <p>This enum is used throughout the system to ensure consistent handling
 * of time-based data across data ingestion, buffering, and model input.
 *
 * <p>Examples:
 * <ul>
 *     <li>{@link #ONE_MINUTE} → 60 seconds</li>
 *     <li>{@link #FIVE_MINUTES} → 300 seconds</li>
 *     <li>{@link #ONE_DAY} → 86400 seconds</li>
 * </ul>
 */
public enum TimeInterval {

    /** One-minute interval (60 seconds). */
    ONE_MINUTE("1", 60),

    /** Five-minute interval (300 seconds). */
    FIVE_MINUTES("5", 5 * 60),

    /** Fifteen-minute interval (900 seconds). */
    FIFTEEN_MINUTES("15", 15 * 60),

    /** Thirty-minute interval (1800 seconds). */
    THIRTY_MINUTES("30", 30 * 60),

    /** One-hour interval (3600 seconds). */
    ONE_HOUR("60", 60 * 60),

    /** One-day interval (86400 seconds). */
    ONE_DAY("D", 24 * 60 * 60);

    /**
     * Resolution string used by the Finnhub API to represent this interval.
     *
     * <p>Examples:
     * <ul>
     *     <li>{@code "1"} for 1-minute</li>
     *     <li>{@code "5"} for 5-minute</li>
     *     <li>{@code "D"} for daily</li>
     * </ul>
     */
    private final String finnhubResolution;

    /**
     * Duration of the interval in seconds.
     */
    private final int seconds;

    /**
     * Constructs a {@code TimeInterval} with a Finnhub resolution string
     * and a duration in seconds.
     *
     * @param finnhubResolution the resolution string used by the Finnhub API
     * @param seconds the duration of the interval in seconds
     */
    TimeInterval(String finnhubResolution, int seconds) {
        this.finnhubResolution = finnhubResolution;
        this.seconds = seconds;
    }

    /**
     * Returns the Finnhub API resolution string for this interval.
     *
     * @return the resolution string (e.g. {@code "1"}, {@code "5"}, {@code "D"})
     */
    public String getFinnhubResolution() {
        return finnhubResolution;
    }

    /**
     * Returns the duration of this interval in seconds.
     *
     * @return the interval length in seconds
     */
    public int getSeconds() {
        return seconds;
    }
}