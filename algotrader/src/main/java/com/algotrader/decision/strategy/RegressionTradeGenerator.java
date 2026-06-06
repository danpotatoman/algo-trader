package com.algotrader.decision.strategy;

import java.util.List;

import com.algotrader.decision.dataobjects.RegressionPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.decision.prediction.provider.RegressionPredictionProvider;
import com.algotrader.decision.strategy.validation.RoundTripTradeValidator;
import com.algotrader.marketdata.model.DataBatch;

/**
 * Trade generator that converts regression model predictions into valid
 * round-trip trades.
 *
 * <p>This generator represents the regression-based trading pipeline: it
 * requests a {@link RegressionPrediction} for a market data batch, passes
 * that prediction to a {@link PredictionInterpreter}, and filters the
 * resulting {@link RoundTripTrade} objects through a
 * {@link RoundTripTradeValidator}.
 *
 * <p>This class is intentionally focused on orchestration. Prediction logic,
 * trade interpretation, and market-session validation are delegated to the
 * injected dependencies.
 *
 * <p>Regression-based trade generation is the intended long-term direction
 * for the project, since regression forecasts can expose richer information
 * than binary classification outputs.
 */
public final class RegressionTradeGenerator implements TradeGenerator {

    private final RegressionPredictionProvider predictionProvider;
    private final PredictionInterpreter<RegressionPrediction> interpreter;
    private final RoundTripTradeValidator tradeValidator;

    /**
    * Creates a regression trade generator.
    *
    * @param predictionProvider provider used to obtain regression predictions
    *        from market data
    * @param interpreter component that converts predictions into proposed
    *        round-trip trades
    * @param tradeValidator validator used to reject trades that violate market
    *        calendar constraints
    * @throws IllegalArgumentException if any dependency is null
    */
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

    /**
     * Generates valid round-trip trades from a market data batch.
     *
     * <p>The method requests a regression prediction, interprets it into proposed
     * trades, and returns only those trades accepted by the configured validator.
     *
     * @param batch market data batch used as model input
     * @return valid round-trip trades generated from the prediction
     * @throws IllegalArgumentException if {@code batch} is null
     * @throws PredictionProviderException if prediction generation fails
     */
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