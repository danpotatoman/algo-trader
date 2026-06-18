package com.algotrader.decision.generator;

import com.algotrader.config.PredictionType;
import com.algotrader.config.StrategyType;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.generator.validation.RoundTripTradeValidator;
import com.algotrader.decision.interpreter.PredictionInterpreter;
import com.algotrader.decision.interpreter.ThresholdClassificationPredictionInterpreter;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.runtime.ResolvedTradingPlan;
import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.interpreter.ClassificationWithVolatilityPredictionInterpreter;

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
 * <p><b>TODO:</b> This factory currently supports classification and
 * classification-with-volatility threshold strategies. Add regression-based
 * trade generator construction as regression strategy support is completed.
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
     * @param tradingPlan resolved trading plan containing endpoint, strategy, and
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

        PredictionType predictionType = tradingPlan.getPredictionType();
        StrategyType strategyType = tradingPlan.getStrategyType();

        if (predictionType == PredictionType.CLASSIFICATION
                && strategyType == StrategyType.THRESHOLD_CLASSIFICATION) {
            return createThresholdClassificationStrategy(tradingPlan);
        }
        if (predictionType == PredictionType.CLASSIFICATION_WITH_VOLATILITY
                && strategyType == StrategyType.THRESHOLD_CLASSIFICATION) {
            return createThresholdClassificationWithVolatilityStrategy(tradingPlan);
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
        PredictionProvider<ClassificationPrediction> provider =
                predictionProviderFactory.createClassificationProvider(
                        tradingPlan
                );

        TradeGeneratorConfig config = tradingPlan.getTradeGeneratorConfig();

        double confidenceThreshold =
                config.getParameters().getRequiredDouble(
                        "confidenceThreshold"
                );

        PredictionInterpreter<ClassificationPrediction> interpreter =
                new ThresholdClassificationPredictionInterpreter(
                        confidenceThreshold,
                        tradingPlan.getStrategyId()
                );

        RoundTripTradeValidator tradeValidator =
                new RoundTripTradeValidator(
                        marketCalendar,
                        tradingPlan.getMinTimeBeforeClose()
                );

        return new GenericTradeGenerator<>(
                provider,
                interpreter,
                tradeValidator
        );
    }

    private TradeGenerator createThresholdClassificationWithVolatilityStrategy(
        ResolvedTradingPlan tradingPlan
    ) {
        PredictionProvider<ClassificationWithVolatilityPrediction> provider =
                predictionProviderFactory.createClassificationWithVolatilityProvider(
                        tradingPlan
                );

        TradeGeneratorConfig config = tradingPlan.getTradeGeneratorConfig();

        double confidenceThreshold =
                config.getParameters().getRequiredDouble(
                        "confidenceThreshold"
                );

        double maxVolatilityThreshold =
                config.getParameters().getRequiredDouble(
                        "maxVolatilityThreshold"
                );

        PredictionInterpreter<ClassificationWithVolatilityPrediction> interpreter =
                new ClassificationWithVolatilityPredictionInterpreter(
                        confidenceThreshold,
                        maxVolatilityThreshold,
                        tradingPlan.getStrategyId()
                );

        RoundTripTradeValidator tradeValidator =
                new RoundTripTradeValidator(
                        marketCalendar,
                        tradingPlan.getMinTimeBeforeClose()
                );

        return new GenericTradeGenerator<>(
                provider,
                interpreter,
                tradeValidator
        );
        }
    
}
