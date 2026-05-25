package com.algotrader.prediction.interpretation;

import com.algotrader.data.dataobjects.ModelPrediction;
import com.algotrader.data.dataobjects.TradeRecommendation;

/**
 * Converts model prediction results into concrete trade recommendations.
 *
 * <p>A {@code PredictionInterpreter} is responsible for strategy logic. It
 * examines a {@link PredictionResult}, including both the model's prediction
 * score and the data batch that produced it, and returns one or more
 * {@link TradeRecommendation} objects.
 *
 * <p>The returned recommendations may include actions scheduled for different
 * timestamps. For example, an interpreter may recommend buying at the current
 * candle timestamp and selling at a later timestamp. The interpreter does not
 * execute trades; it only produces recommendations.
 *
 * <p>Actual execution, timing, account state checks, and broker or paper-trading
 * behavior are handled by a separate trade service or trade maker.
 */
public interface PredictionInterpreter<T extends ModelPrediction> {

    /**
     * Generates trade recommendations from a model prediction result.
     *
     * <p>The returned array may contain multiple timestamped recommendations,
     * such as one recommendation to buy and another recommendation to sell.
     * The trade execution layer is responsible for performing each recommendation.
     *
     * @param prediction the prediction result containing the input data batch and
     *                   corresponding model prediction score
     * @return an array of trade recommendations derived from the prediction result
     * @throws IllegalArgumentException if {@code result} is null
     */
    TradeRecommendation[] getRecommendations(T prediction);
}