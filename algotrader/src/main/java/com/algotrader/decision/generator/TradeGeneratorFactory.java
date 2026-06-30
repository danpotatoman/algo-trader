package com.algotrader.decision.generator;

import java.util.List;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.PredictionType;
import com.algotrader.config.TradeGeneratorType;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.generator.validation.RoundTripTradeValidator;
import com.algotrader.decision.planner.ClassificationWithVolatilityTradePlanner;
import com.algotrader.decision.planner.ThresholdClassificationTradePlanner;
import com.algotrader.decision.planner.TradePlanner;
import com.algotrader.decision.planner.VolatilityScaledTradePlanner;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.portfolio.PortfolioView;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.runtime.ResolvedTradingPlan;


/**
 * Factory for constructing {@link TradeGenerator} instances from resolved
 * trading configuration.
 *
 * <p>This factory selects the appropriate trade generation pipeline based on
 * the prediction type and trade generator type declared by a
 * {@link ResolvedTradingPlan}.
 *
 * <p>Created trade generators are assembled from a prediction provider, a
 * trade planner, and a round-trip trade validator.
 *
 * <p>Classification and classification-with-volatility pipelines are
 * supported. Regression prediction types remain placeholders and are not yet
 * supported end-to-end.
 */
public final class TradeGeneratorFactory {

    private final PredictionProviderFactory predictionProviderFactory;
    private final MarketCalendar marketCalendar;
    private final PriceProvider priceProvider;

    /**
     * Creates a trade generator factory.
     *
     * @param predictionProviderFactory factory used to construct prediction
     *        providers
     * @param marketCalendar market calendar used to validate generated trades
     * @param priceProvider price source used for position sizing
     * @throws IllegalArgumentException if any dependency is null
     */
    public TradeGeneratorFactory(
            PredictionProviderFactory predictionProviderFactory,
            MarketCalendar marketCalendar,
            PriceProvider priceProvider
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

        if (priceProvider == null) {
            throw new IllegalArgumentException(
                "PriceProvider cannot be null."
            );
        }

        this.predictionProviderFactory = predictionProviderFactory;
        this.marketCalendar = marketCalendar;
        this.priceProvider = priceProvider;
    }

    /**
     * Creates a trade generator for the supplied resolved trading plan.
     *
     * @param tradingPlan resolved trading plan containing endpoint, trade
     *        generator, and session configuration
     * @param portfolioView portfolio view used by generators that size
     *        positions from available cash; may be null for generators that do
     *        not require it
     * @return a trade generator matching the configured prediction and
     *         generator types
     * @throws IllegalArgumentException if {@code tradingPlan} is null or if the
     *         configured prediction/generator combination is unsupported, or
     *         if a required portfolio view is absent
     */
    public TradeGenerator create(
            ResolvedTradingPlan tradingPlan, PortfolioView portfolioView
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                "ResolvedTradingPlan cannot be null."
            );
        }

        PredictionType predictionType = tradingPlan.getPredictionType();
        TradeGeneratorType generatorType = tradingPlan.getStrategyType();

        if (predictionType == PredictionType.CLASSIFICATION
                && generatorType == TradeGeneratorType.THRESHOLD) {
            return createThresholdClassification(tradingPlan);
        }
        if (predictionType == PredictionType.CLASSIFICATION_WITH_VOLATILITY
                && generatorType == TradeGeneratorType.THRESHOLD) {
            return createVolatilityFilteredClassification(tradingPlan);
        }
        if (predictionType == PredictionType.CLASSIFICATION_WITH_VOLATILITY
                && generatorType == TradeGeneratorType.VOLATILITY_SCALED_THRESHOLD) {
            if (portfolioView == null) {
                throw new IllegalArgumentException(
                        "PortfolioView cannot be null for volatility-scaled-threshold trade generator."
                );
            }
            return createVolatilityScaledClassification(tradingPlan, portfolioView);
        }

        throw new IllegalArgumentException(
                "Unsupported model/trade generator combination: "
                        + predictionType
                        + " / "
                        + generatorType
        );
    }

    public PortfolioDecisionGenerator createPortfolioDecisionGenerator(
                ResolvedTradingPlan tradingPlan, PortfolioView portfolioView) {
        if (tradingPlan == null) {
                throw new IllegalArgumentException(
                "ResolvedTradingPlan cannot be null."
                );
        }
        if (portfolioView == null) {
                throw new IllegalArgumentException(
                        "PortfolioView cannot be null for volatility-scaled-threshold trade generator."
                );
        }

        PredictionType predictionType = tradingPlan.getPredictionType();
        TradeGeneratorType generatorType = tradingPlan.getStrategyType();

        if (predictionType == PredictionType.BATCH_CLASSIFICATION_WITH_VOLATILITY
                && generatorType == TradeGeneratorType.VOLATILITY_SCALED_THRESHOLD) {
            return createClassificationVolatilityPortfolioDecisionGenerator(tradingPlan, portfolioView);
        }

        throw new IllegalArgumentException(
                "Unsupported model/trade generator combination: "
                        + predictionType
                        + " / "
                        + generatorType
        );
    }

    /**
     * Creates the classification threshold trade generation pipeline.
     *
     * @param tradingPlan resolved trading plan containing the required generator
     *        parameters
     * @return a configured classification trade generator
     */
    private TradeGenerator createThresholdClassification(
            ResolvedTradingPlan tradingPlan
        ) {
        PredictionProvider<DataBatch, ClassificationPrediction> provider =
                predictionProviderFactory.createClassificationProvider(
                        tradingPlan
                );

        TradeGeneratorConfig config = tradingPlan.getTradeGeneratorConfig();

        double confidenceThreshold =
                config.getParameters().getRequiredDouble(
                        "confidenceThreshold"
                );

        TradePlanner<ClassificationPrediction> tradePlanner =
                new ThresholdClassificationTradePlanner(
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
                tradePlanner,
                tradeValidator
        );
    }

    /**
     * Creates the volatility-aware classification threshold pipeline, using
     * volatility as a simple threshold filter.
     *
     * @param tradingPlan resolved trading plan containing confidence and
     *        volatility thresholds
     * @return a configured classification-with-volatility trade generator
     */
    private TradeGenerator createVolatilityFilteredClassification(
        ResolvedTradingPlan tradingPlan
    ) {
        PredictionProvider<DataBatch, ClassificationWithVolatilityPrediction> provider =
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

        TradePlanner<ClassificationWithVolatilityPrediction> tradePlanner =
                new ClassificationWithVolatilityTradePlanner(
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
                tradePlanner,
                tradeValidator
        );
    }

    /**
     * Creates the volatility-scaled classification pipeline.
     *
     * @param tradingPlan resolved trading plan containing confidence and
     *        position-sizing parameters
     * @param portfolio account supplying available cash for position sizing
     * @return a configured classification-with-volatility trade generator
     */
    private TradeGenerator createVolatilityScaledClassification(
        ResolvedTradingPlan tradingPlan, PortfolioView portfolio
    ) {
        PredictionProvider<DataBatch, ClassificationWithVolatilityPrediction> provider =
                predictionProviderFactory.createClassificationWithVolatilityProvider(
                        tradingPlan
                );

        EndpointConfig endpointConfig = tradingPlan.getEndpointConfig();

        TradeGeneratorConfig tradeGeneratorConfig = tradingPlan.getTradeGeneratorConfig();

        double volatilityMean =
                endpointConfig.getRequiredOutputStatistic(
                        "volatilityMean"
                );

        double volatilityStd =
                endpointConfig.getRequiredOutputStatistic(
                        "volatilityStd"
                );

        double minConfidenceThreshold = 
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "minConfidenceThreshold"
                );
        
        double minPositionFraction = 
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "minPositionFraction"
                );

        double maxPositionFraction = 
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "maxPositionFraction"
                );

        TradePlanner<ClassificationWithVolatilityPrediction> tradePlanner =
                new VolatilityScaledTradePlanner(
                        portfolio,
                        priceProvider,
                        tradingPlan.getStrategyId(),
                        volatilityMean,
                        volatilityStd,
                        minConfidenceThreshold,
                        minPositionFraction,
                        maxPositionFraction
                );

        RoundTripTradeValidator tradeValidator =
                new RoundTripTradeValidator(
                        marketCalendar,
                        tradingPlan.getMinTimeBeforeClose()
                );

        return new GenericTradeGenerator<>(
                provider,
                tradePlanner,
                tradeValidator
        );
    }

    private ClassificationVolatilityPortfolioDecisionGenerator createClassificationVolatilityPortfolioDecisionGenerator(
                ResolvedTradingPlan tradingPlan, PortfolioView portfolioView) {
        PredictionProvider<List<DataBatch>, List<ClassificationWithVolatilityPrediction>> predictionProvider
                    = predictionProviderFactory.createBatchClassificationVolatilityProvider(tradingPlan);

        TradeGeneratorConfig tradeGeneratorConfig = tradingPlan.getTradeGeneratorConfig();

        double minConfidenceThreshold = 
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "minConfidenceThreshold"
                );

        double cashAllocationFraction =
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "cashAllocationFractionPerCycle"
                );

        double maxAllocationFractionPerTicker =
                tradeGeneratorConfig.getParameters().getRequiredDouble(
                        "maxAllocationFractionPerTickerPerCycle"
                );

        return new ClassificationVolatilityPortfolioDecisionGenerator(
                predictionProvider,
                minConfidenceThreshold,
                cashAllocationFraction,
                maxAllocationFractionPerTicker,
                tradingPlan.getStrategyId()
                );
    }
}
