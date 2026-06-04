package com.algotrader.decision.strategy;

import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.prediction.provider.ClassificationPredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
import com.algotrader.decision.strategy.validation.RoundTripTradeValidator;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link TradeGenerator} instances from model,
 * strategy, and trading plan configuration.
 */
public final class TradeGeneratorFactory {

    private final PredictionProviderFactory predictionProviderFactory;
    private final MarketCalendar marketCalendar;

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

    public TradeGenerator create(
            ResolvedTradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingPlan cannot be null."
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

    private TradeGenerator createThresholdClassificationStrategy(
            ResolvedTradingPlan tradingPlan
    ) {
        ClassificationPredictionProvider provider =
                predictionProviderFactory.createClassificationProvider(
                        tradingPlan
                );

        PredictionInterpreter<ClassificationPrediction> interpreter =
                new SimpleClassificationPredictionInterpreter(
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