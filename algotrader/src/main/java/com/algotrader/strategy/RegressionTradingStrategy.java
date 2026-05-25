package com.algotrader.strategy;

import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.RegressionPrediction;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.prediction.RegressionPredictionProvider;
import com.algotrader.prediction.interpretation.PredictionInterpreter;

/**
 * Trading strategy that uses a regression model prediction and converts it
 * into trade recommendations.
 */
public final class RegressionTradingStrategy implements TradingStrategy {

    private final RegressionPredictionProvider predictionProvider;
    private final PredictionInterpreter<RegressionPrediction> interpreter;

    public RegressionTradingStrategy(
            RegressionPredictionProvider predictionProvider,
            PredictionInterpreter<RegressionPrediction> interpreter
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException("RegressionPredictionProvider cannot be null.");
        }

        if (interpreter == null) {
            throw new IllegalArgumentException("PredictionInterpreter cannot be null.");
        }

        this.predictionProvider = predictionProvider;
        this.interpreter = interpreter;
    }

    @Override
    public TradeRecommendation[] generateRecommendations(
            DataBatch batch
    ) throws PredictionProviderException {
        if (batch == null) {
            throw new IllegalArgumentException("DataBatch cannot be null.");
        }

        RegressionPrediction prediction =
                predictionProvider.makePrediction(batch);

        return interpreter.getRecommendations(prediction);
    }
}