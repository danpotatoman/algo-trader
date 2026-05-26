package com.algotrader.strategy;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.TradingStrategyConfig;
import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderFactory;
import com.algotrader.prediction.interpretation.PredictionInterpreter;
import com.algotrader.prediction.interpretation.SimpleClassificationPredictionInterpreter;

/**
 * Factory for constructing {@link TradingStrategy} instances from model and
 * strategy configuration.
 */
public final class TradingStrategyFactory {

    private final PredictionProviderFactory predictionProviderFactory;

    public TradingStrategyFactory(
            PredictionProviderFactory predictionProviderFactory
    ) {
        if (predictionProviderFactory == null) {
            throw new IllegalArgumentException(
                    "PredictionProviderFactory cannot be null."
            );
        }

        this.predictionProviderFactory = predictionProviderFactory;
    }

    public TradingStrategy create(
            ModelConfig modelConfig,
            TradingStrategyConfig strategyConfig
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

        String predictionType =
                modelConfig.getPredictionType().toUpperCase();

        String strategyType =
                strategyConfig.getStrategyType().toUpperCase();

        if (predictionType.equals("CLASSIFICATION")
                && strategyType.equals("THRESHOLD_CLASSIFICATION")) {
            return createThresholdClassificationStrategy(
                    modelConfig,
                    strategyConfig
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
            TradingStrategyConfig strategyConfig
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
                                .getHorizonMinutes()
                );

        return new ClassificationTradingStrategy(
                provider,
                interpreter
        );
    }
}