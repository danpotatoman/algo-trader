package com.algotrader.strategy;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.TradingPlan;
import com.algotrader.config.TradingStrategyConfig;
import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.market.MarketCalendar;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderFactory;
import com.algotrader.prediction.interpretation.PredictionInterpreter;
import com.algotrader.prediction.interpretation.SimpleClassificationPredictionInterpreter;
import com.algotrader.strategy.validation.RoundTripTradeValidator;

/**
 * Factory for constructing {@link TradingStrategy} instances from model,
 * strategy, and trading plan configuration.
 */
public final class TradingStrategyFactory {

    private final PredictionProviderFactory predictionProviderFactory;
    private final MarketCalendar marketCalendar;

    public TradingStrategyFactory(
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

    public TradingStrategy create(
            ModelConfig modelConfig,
            TradingStrategyConfig strategyConfig,
            TradingPlan tradingPlan
    ) {
        if (modelConfig == null) {
            throw new IllegalArgumentException(
                    "ModelConfig cannot be null."
            );
        }

        if (strategyConfig == null) {
            throw new IllegalArgumentException(
                    "TradingStrategyConfig cannot be null."
            );
        }

        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingPlan cannot be null."
            );
        }

        String predictionType =
                modelConfig.getPredictionType().toUpperCase();

        String strategyType =
                strategyConfig.getStrategyType().toUpperCase();

        if (predictionType.equals("CLASSIFICATION")
                && strategyType.equals("THRESHOLD_CLASSIFICATION")) {
            return createThresholdClassificationStrategy(
                    modelConfig,
                    strategyConfig,
                    tradingPlan
            );
        }

        throw new IllegalArgumentException(
                "Unsupported model/strategy combination: "
                        + predictionType
                        + " / "
                        + strategyType
        );
    }

    private TradingStrategy createThresholdClassificationStrategy(
            ModelConfig modelConfig,
            TradingStrategyConfig strategyConfig,
            TradingPlan tradingPlan
    ) {
        ClassificationPredictionProvider provider =
                predictionProviderFactory.createClassificationProvider(
                        modelConfig
                );

        PredictionInterpreter<ClassificationPrediction> interpreter =
                new SimpleClassificationPredictionInterpreter(
                        strategyConfig.getParameters()
                                .getConfidenceThreshold(),
                        modelConfig.getOutput()
                                .getHorizonMinutes(),
                        strategyConfig.getStrategyId()
                );

        RoundTripTradeValidator tradeValidator =
                new RoundTripTradeValidator(
                        marketCalendar,
                        tradingPlan.getMinTimeBeforeClose()
                );

        return new ClassificationTradingStrategy(
                provider,
                interpreter,
                tradeValidator
        );
    }
}