package com.algotrader.strategy;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.prediction.interpretation.PredictionInterpreter;

/**
 * Trading strategy that uses a classification model prediction and converts it
 * into trade recommendations.
 */
public final class ClassificationTradingStrategy implements TradingStrategy {

    private final ClassificationPredictionProvider predictionProvider;
    private final PredictionInterpreter<ClassificationPrediction> interpreter;

    public ClassificationTradingStrategy(
            ClassificationPredictionProvider predictionProvider,
            PredictionInterpreter<ClassificationPrediction> interpreter
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException("ClassificationPredictionProvider cannot be null.");
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

        ClassificationPrediction prediction =
                predictionProvider.makePrediction(batch);

        return interpreter.getRecommendations(prediction);
    }
}