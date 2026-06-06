package com.algotrader.logging;

/**
 * Responsible for recording completed trading cycles.
 *
 * <p>Implementations may persist {@link TradingCycleLog} objects to a variety
 * of destinations, including:
 * <ul>
 *     <li>Console output</li>
 *     <li>JSON files</li>
 *     <li>Databases</li>
 *     <li>Remote analytics services</li>
 *     <li>In-memory collections for testing</li>
 * </ul>
 *
 * <p>This interface represents the observability layer of the trading
 * system. Logged trading cycles can be used for debugging, auditing,
 * performance analysis, and backtesting evaluation.
 *
 * <p>A {@link TradingCycleLog} captures the outcome of a completed trading
 * cycle, including cycle metadata, execution details, and generated trade
 * actions.
 */
public interface TradingCycleLogger {

    /**
     * Records a completed trading cycle.
     *
     * @param tradingCycleLog trading cycle log to record
     * @throws IllegalArgumentException if {@code tradingCycleLog} is null
     */
    void log(TradingCycleLog tradingCycleLog);
}