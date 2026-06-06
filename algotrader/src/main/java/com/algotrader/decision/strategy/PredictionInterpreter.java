package com.algotrader.decision.strategy;

import java.util.List;

import com.algotrader.decision.dataobjects.ModelPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Strategy component that converts model predictions into proposed
 * round-trip trades.
 *
 * <p>A {@code PredictionInterpreter} contains the decision-making logic of
 * the trading system. It examines a {@link ModelPrediction} and determines
 * whether any trading opportunities exist, returning zero or more
 * {@link RoundTripTrade} objects.
 *
 * <p>The interpreter is responsible for deciding <em>what</em> trades should
 * be made, but not whether those trades are valid or how they are executed.
 * Market-session validation, execution scheduling, broker interaction, and
 * position management are handled by separate components.
 *
 * <p>Implementations may apply confidence thresholds, forecast analysis,
 * risk controls, or other strategy-specific rules when generating trades.
 *
 * @param <T> the prediction type interpreted by this strategy
 */
public interface PredictionInterpreter<T extends ModelPrediction> {

    /**
     * Generates proposed round-trip trades from a model prediction.
     *
     * <p>An implementation may return an empty list if the prediction does
     * not satisfy the strategy's trading criteria.
     *
     * @param prediction prediction to interpret
     * @return proposed trades derived from the prediction
     * @throws IllegalArgumentException if {@code prediction} is null
     */
    List<RoundTripTrade> getTrades(T prediction);
}