package com.algotrader.decision.strategy;

import java.util.List;

import com.algotrader.decision.dataobjects.RegressionPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.decision.prediction.provider.RegressionPredictionProvider;
import com.algotrader.decision.strategy.validation.RoundTripTradeValidator;
import com.algotrader.marketdata.model.DataBatch;

/**
 * Trading strategy that uses a regression model prediction and converts it
 * into valid round-trip trades.
 */
public final class RegressionTradeGenerator implements TradeGenerator {

    private final RegressionPredictionProvider predictionProvider;
    private final PredictionInterpreter<RegressionPrediction> interpreter;
    private final RoundTripTradeValidator tradeValidator;

    public RegressionTradeGenerator(
            RegressionPredictionProvider predictionProvider,
            PredictionInterpreter<RegressionPrediction> interpreter,
            RoundTripTradeValidator tradeValidator
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException(
                    "RegressionPredictionProvider cannot be null."
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

        RegressionPrediction prediction =
                predictionProvider.makePrediction(batch);

        return interpreter.getTrades(prediction)
                .stream()
                .filter(tradeValidator::isValid)
                .toList();
    }
}