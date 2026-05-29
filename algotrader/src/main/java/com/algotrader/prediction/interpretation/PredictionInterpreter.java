package com.algotrader.prediction.interpretation;

import java.util.List;

import com.algotrader.data.dataobjects.ModelPrediction;
import com.algotrader.strategy.RoundTripTrade;

/**
 * Converts model prediction results into complete round-trip trades.
 *
 * <p>A {@code PredictionInterpreter} is responsible for strategy logic. It
 * examines a {@link ModelPrediction} and returns one or more
 * {@link RoundTripTrade} objects.
 *
 * <p>A round-trip trade represents a complete trading idea consisting of an
 * entry and exit of the same position. Implementations should avoid producing
 * trades that cannot reasonably be completed, such as trades whose exit would
 * occur outside market hours.
 *
 * <p>The interpreter does not execute trades. It only determines which trades
 * should be attempted based on model output and strategy rules.
 *
 * <p>Actual execution, timing, account state checks, broker interaction, and
 * position management are handled by separate execution components.
 *
 * @param <T> the prediction type interpreted by this strategy
 */
public interface PredictionInterpreter<T extends ModelPrediction> {

    /**
     * Generates round-trip trades from a model prediction.
     *
     * <p>The returned trades represent complete entry/exit plans. An
     * implementation may return an empty list if no trade opportunities are
     * identified.
     *
     * @param prediction the prediction result used to generate trades
     * @return a list of round-trip trades derived from the prediction
     * @throws IllegalArgumentException if {@code prediction} is null
     */
    List<RoundTripTrade> getTrades(T prediction);
}