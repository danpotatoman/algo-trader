package com.algotrader.strategy;

import java.util.List;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.prediction.interpretation.PredictionInterpreter;
import com.algotrader.strategy.validation.RoundTripTradeValidator;

/**
 * Trading strategy that uses a classification model prediction and converts it
 * into valid round-trip trades.
 */
public final class ClassificationTradingStrategy implements TradingStrategy {

    private final ClassificationPredictionProvider predictionProvider;
    private final PredictionInterpreter<ClassificationPrediction> interpreter;
    private final RoundTripTradeValidator tradeValidator;

    public ClassificationTradingStrategy(
            ClassificationPredictionProvider predictionProvider,
            PredictionInterpreter<ClassificationPrediction> interpreter,
            RoundTripTradeValidator tradeValidator
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException(
                    "ClassificationPredictionProvider cannot be null."
            );
        }

        if (interpreter == null) {
            throw new IllegalArgumentException(
                    "PredictionInterpreter cannot be null."
            );
        }

        if (tradeValidator == null) {
            throw new IllegalArgumentException(
                    "RoundTripTradeValidator cannot be null."
            );
        }

        this.predictionProvider = predictionProvider;
        this.interpreter = interpreter;
        this.tradeValidator = tradeValidator;
    }

    @Override
    public List<RoundTripTrade> generateTrades(
            DataBatch batch
    ) throws PredictionProviderException {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        ClassificationPrediction prediction =
                predictionProvider.makePrediction(batch);

        return interpreter.getTrades(prediction)
                .stream()
                .filter(tradeValidator::isValid)
                .toList();
    }
}