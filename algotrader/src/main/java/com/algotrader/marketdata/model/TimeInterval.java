package com.algotrader.marketdata.model;

import java.time.Duration;

/**
 * Represents supported time intervals for OHLCV candlestick data.
 *
 * <p>Each interval defines:
 * <ul>
 *     <li>A resolution string used by external APIs</li>
 *     <li>The duration of the interval in seconds</li>
 * </ul>
 *
 * <p>This enum is used throughout the system to ensure consistent handling
 * of time-based market data across data ingestion, caching, batching,
 * prediction, and execution workflows.
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
     * Resolution string used by external market data providers.
     */
    private final String finnhubResolution;

    /**
     * Duration of the interval in seconds.
     */
    private final int seconds;

    /**
     * Creates a time interval definition.
     *
     * @param finnhubResolution provider-specific resolution string
     * @param seconds interval duration in seconds
     */
    TimeInterval(String finnhubResolution, int seconds) {
        this.finnhubResolution = finnhubResolution;
        this.seconds = seconds;
    }

    /**
     * Returns the resolution string used by compatible market data providers.
     *
     * @return provider-specific resolution string
     *
     * TODO: Consider renaming this method if the project adopts
     * different market data providers.
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

    /**
     * Returns the duration represented by this interval.
     *
     * @return interval duration
     */
    public Duration getDuration() {
        return Duration.ofSeconds(seconds);
    }
}