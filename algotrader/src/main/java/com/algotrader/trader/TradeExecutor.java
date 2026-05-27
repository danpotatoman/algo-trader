package com.algotrader.trader;

import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradingCycleLog.ActionLog;
import com.algotrader.prediction.interpretation.PredictionInterpreter;

/**
 * Responsible for handling and executing trade recommendations produced by a
 * {@link PredictionInterpreter}.
 *
 * <p>A {@code TradeExecutor} receives a {@link TradeRecommendation} and
 * determines how and when to act on it. This may include:
 * <ul>
 *     <li>Scheduling trades for future execution</li>
 *     <li>Checking account state (positions, buying power, etc.)</li>
 *     <li>Executing trades via a broker or paper trading system</li>
 *     <li>Logging or simulating trade activity</li>
 * </ul>
 *
 * <p>This interface does not prescribe how recommendations are executed;
 * implementations may vary (e.g. immediate execution, scheduled execution,
 * simulation, or filtering).
 */
public interface TradeExecutor {

    /**
     * Handles a single trade recommendation.
     *
     * <p>The executor may execute the recommendation immediately, schedule
     * it for future execution based on its timestamp, or ignore it
     * depending on its internal logic and constraints.
     *
     * @param recommendation the trade recommendation to handle
     * @return a TradeExecutionLog describing the execution result
     * @throws IllegalArgumentException if {@code recommendation} is null
     */
    ActionLog handleRecommendation(
            TradeRecommendation recommendation
    );
}