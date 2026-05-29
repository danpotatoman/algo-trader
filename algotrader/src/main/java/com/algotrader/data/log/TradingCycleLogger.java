package com.algotrader.data.log;

/**
 * Responsible for logging or persisting {@link TradingCycleLog} objects.
 *
 * <p>Implementations may write trading cycle logs to:
 * <ul>
 *     <li>Console output</li>
 *     <li>JSON files</li>
 *     <li>Databases</li>
 *     <li>Remote analytics services</li>
 *     <li>In-memory collections for testing</li>
 * </ul>
 *
 * <p>A trading cycle log represents the complete result of one historical
 * or live trading cycle, including metadata, generated recommendations,
 * and execution information.
 */
public interface TradingCycleLogger {

    /**
     * Logs a completed trading cycle.
     *
     * @param tradingCycleLog the trading cycle log to record
     * @throws IllegalArgumentException if tradingCycleLog is null
     */
    void log(TradingCycleLog tradingCycleLog);
}