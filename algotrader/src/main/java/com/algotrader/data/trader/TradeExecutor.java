package com.algotrader.data.trader;

import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.prediction.interpretation.PredictionInterpreter;

/**
 * Responsible for handling and executing trade recommendations produced by a
 * {@link PredictionInterpreter}.
 *
 * <p>A {@code TradeExecutor} receives one or more {@link TradeRecommendation}
 * objects and determines how and when to act on them. This may include:
 * <ul>
 *     <li>Scheduling trades for future execution</li>
 *     <li>Checking account state (positions, buying power, etc.)</li>
 *     <li>Executing trades via a broker or paper trading system</li>
 *     <li>Logging or simulating trade activity</li>
 * </ul>
 *
 * <p>The executor is given both the recommendations and the original
 * {@link PredictionResult} to provide additional context if needed.
 *
 * <p>This interface does not prescribe how recommendations are executed;
 * implementations may vary (e.g. immediate execution, scheduled execution,
 * simulation, or filtering).
 */
public interface TradeExecutor {

    /**
     * Handles a set of trade recommendations.
     *
     * <p>The executor may execute the recommendations immediately, schedule
     * them for future execution based on their timestamps, or ignore them
     * depending on its internal logic and constraints.
     *
     * @param recommendations the array of trade recommendations to handle
     *
     * @throws IllegalArgumentException if {@code recommendations} is null
     */
    void handleRecommendations(
            TradeRecommendation[] recommendations
    );
}