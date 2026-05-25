package com.algotrader.data.log;

import com.algotrader.data.dataobjects.TradeExecutionLog;
import com.algotrader.data.trader.TradeExecutor;

/**
 * Responsible for recording or persisting trade execution information.
 *
 * <p>A {@code TradeLogger} consumes {@link TradeExecutionLog} objects produced
 * by a {@link TradeExecutor}. Implementations may choose to:
 * <ul>
 *     <li>Print logs to the console</li>
 *     <li>Write logs to CSV or JSON files</li>
 *     <li>Store logs in a database</li>
 *     <li>Maintain logs in memory for analysis or testing</li>
 * </ul>
 *
 * <p>This interface intentionally separates logging behavior from trade
 * execution logic.
 */
public interface TradeLogger {

    /**
     * Records a trade execution log.
     *
     * @param log the trade execution log to record
     * @throws IllegalArgumentException if {@code log} is null
     */
    void log(TradeExecutionLog log);
}