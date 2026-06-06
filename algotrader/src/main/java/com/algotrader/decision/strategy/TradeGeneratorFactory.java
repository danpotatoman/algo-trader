package com.algotrader.decision.strategy;

import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.prediction.provider.ClassificationPredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
import com.algotrader.decision.strategy.validation.RoundTripTradeValidator;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link TradeGenerator} instances from resolved
 * trading configuration.
 *
 * <p>This factory selects the appropriate trade generation pipeline based on
 * the prediction type and strategy type declared by a
 * {@link ResolvedTradingPlan}.
 *
 * <p>Created trade generators are assembled from a prediction provider, a
 * prediction interpreter, and a round-trip trade validator.
 *
 * <p><b>TODO:</b> This factory currently supports only the classification
 * threshold strategy. Add regression-based trade generator construction as
 * regression strategy support is completed.
 */
public final class TradeGeneratorFactory {

    private final PredictionProviderFactory predictionProviderFactory;
    private final MarketCalendar marketCalendar;

    /**
     * Creates a trade generator factory.
     *
     * @param predictionProviderFactory factory used to construct prediction
     *        providers
     * @param marketCalendar market calendar used to validate generated trades
     * @throws IllegalArgumentException if any dependency is null
     */
    public TradeGeneratorFactory(
            PredictionProviderFactory predictionProviderFactory,
            MarketCalendar marketCalendar
    ) {
        if (predictionProviderFactory == null) {
            throw new IllegalArgumentException(
                    "PredictionProviderFactory cannot be null."
            );
        }

        if (marketCalendar == null) {
            throw new IllegalArgumentException(
                    "MarketCalendar cannot be null."
            );
        }

        this.predictionProviderFactory = predictionProviderFactory;
        this.marketCalendar = marketCalendar;
    }

    /**
     * Creates a trade generator for the supplied resolved trading plan.
     *
     * @param tradingPlan resolved trading plan containing model, strategy, and
     *        session configuration
     * @return a trade generator matching the configured prediction and strategy
     *         types
     * @throws IllegalArgumentException if {@code tradingPlan} is null or if the
     *         configured prediction/strategy combination is unsupported
     */
    public TradeGenerator create(
            ResolvedTradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "ResolvedTradingPlan cannot be null."
            );
        }

        String predictionType = tradingPlan.getPredictionType().toUpperCase();

        String strategyType = tradingPlan.getStrategyType().toUpperCase();

        if (predictionType.equals("CLASSIFICATION")
                && strategyType.equals("THRESHOLD_CLASSIFICATION")) {
            return createThresholdClassificationStrategy(tradingPlan);
        }

        throw new IllegalArgumentException(
                "Unsupported model/strategy combination: "
                        + predictionType
                        + " / "
                        + strategyType
        );
    }

    /**
     * Creates the classification threshold trade generation pipeline.
     *
     * @param tradingPlan resolved trading plan containing the required strategy
     *        parameters
     * @return a configured classification trade generator
     */
    private TradeGenerator createThresholdClassificationStrategy(
            ResolvedTradingPlan tradingPlan
    ) {
        ClassificationPredictionProvider provider =
                predictionProviderFactory.createClassificationProvider(
                        tradingPlan
                );

        PredictionInterpreter<ClassificationPrediction> interpreter =
                new ThresholdClassificationPredictionInterpreter(
                        tradingPlan.getConfidenceThreshold(),
                        tradingPlan.getHorizonMinutes(),
                        tradingPlan.getStrategyId()
                );

        RoundTripTradeValidator tradeValidator =
                new RoundTripTradeValidator(
                        marketCalendar,
                        tradingPlan.getMinTimeBeforeClose()
                );

        return new ClassificationTradeGenerator(
                provider,
                interpreter,
                tradeValidator
        );
    }
}