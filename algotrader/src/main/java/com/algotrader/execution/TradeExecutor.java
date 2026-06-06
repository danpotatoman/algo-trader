package com.algotrader.execution;

import com.algotrader.decision.dataobjects.TradeRecommendation;
import com.algotrader.logging.TradingCycleLog.ActionLog;

/**
 * Responsible for executing or simulating trade recommendations.
 *
 * <p>A {@code TradeExecutor} receives a {@link TradeRecommendation} and
 * determines how it should be processed. Implementations may:
 * <ul>
 *     <li>Execute trades immediately</li>
 *     <li>Schedule trades for future execution</li>
 *     <li>Simulate execution using historical market data</li>
 *     <li>Submit orders to a broker or exchange</li>
 *     <li>Reject or filter recommendations based on execution constraints</li>
 * </ul>
 *
 * <p>This interface represents the execution layer of the trading pipeline:
 *
 * <pre>
 * TradeRecommendation -> Execution -> ActionLog
 * </pre>
 *
 * <p>The interface does not prescribe how recommendations are executed.
 * Different implementations may support paper trading, backtesting, live
 * broker integration, delayed execution, or other execution models.
 */
public interface TradeExecutor {

    /**
     * Handles a single trade recommendation.
     *
     * <p>The recommendation may be executed immediately, scheduled for later
     * execution, simulated, or ignored depending on the implementation.
     *
     * @param recommendation trade recommendation to handle
     * @return an action log describing the resulting execution
     * @throws IllegalArgumentException if {@code recommendation} is null
     */
    ActionLog handleRecommendation(
            TradeRecommendation recommendation
    );
}