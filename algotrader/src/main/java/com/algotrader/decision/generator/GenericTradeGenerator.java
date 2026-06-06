package com.algotrader.decision.generator;

import java.util.List;

import com.algotrader.decision.dataobjects.ModelPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.generator.validation.RoundTripTradeValidator;
import com.algotrader.decision.interpreter.PredictionInterpreter;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.marketdata.model.DataBatch;

/**
 * Generic trade generator that converts model predictions into valid
 * round-trip trades.
 *
 * <p>This generator represents the common trading pipeline: it requests a
 * model prediction for a market data batch, passes that prediction to a
 * {@link PredictionInterpreter}, and filters the resulting
 * {@link RoundTripTrade} objects through a {@link RoundTripTradeValidator}.
 *
 * <p>This class is intentionally focused on orchestration. Prediction logic,
 * prediction interpretation, and market-session validation are delegated to the
 * injected dependencies.
 *
 * @param <T> concrete prediction type produced and interpreted by this generator
 */
public final class GenericTradeGenerator<T extends ModelPrediction>
        implements TradeGenerator {

    private final PredictionProvider<T> predictionProvider;
    private final PredictionInterpreter<T> interpreter;
    private final RoundTripTradeValidator tradeValidator;

    /**
     * Creates a generic trade generator.
     *
     * @param predictionProvider provider used to obtain predictions from market data
     * @param interpreter component that converts predictions into proposed
     *        round-trip trades
     * @param tradeValidator validator used to reject trades that violate market
     *        calendar constraints
     * @throws IllegalArgumentException if any dependency is null
     */
    public GenericTradeGenerator(
            PredictionProvider<T> predictionProvider,
            PredictionInterpreter<T> interpreter,
            RoundTripTradeValidator tradeValidator
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException(
                    "PredictionProvider cannot be null."
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
     * <p>The method requests a prediction, interprets it into proposed trades,
     * and returns only those trades accepted by the configured validator.
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

        T prediction = predictionProvider.makePrediction(batch);

        return interpreter.getTrades(prediction)
                .stream()
                .filter(tradeValidator::isValid)
                .toList();
    }
}