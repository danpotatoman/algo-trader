package com.algotrader.decision.prediction.provider;

import com.algotrader.config.ModelConfig;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing prediction providers from {@link ModelConfig}
 * definitions.
 */
public final class PredictionProviderFactory {

    public PredictionProviderFactory() {}

    /**
     * Creates a classification prediction provider from the supplied model config.
     *
     * @param modelConfig the model configuration
     * @return a classification prediction provider
     */
    public ClassificationPredictionProvider
            createClassificationProvider(
                    ResolvedTradingPlan tradingPlan
            ) {

        ModelConfig modelConfig = tradingPlan.getModelConfig();

        validateModelConfig(modelConfig, "CLASSIFICATION");

        PythonPredictionClient client =
                new PythonPredictionClient(
                        modelConfig.getEndpoint()
                );

        return new PythonClassificationPredictionProvider(client);
    }

    /**
     * Creates a regression prediction provider from the supplied model config.
     *
     * @param modelConfig the model configuration
     * @return a regression prediction provider
     */
    public RegressionPredictionProvider
            createRegressionProvider(
                    ModelConfig modelConfig
            ) {

        validateModelConfig(modelConfig, "REGRESSION");

        PythonPredictionClient client =
                new PythonPredictionClient(
                        modelConfig.getEndpoint()
                );

        return new PythonRegressionPredictionProvider(client);
    }

    /**
     * Validates that the supplied model config matches the expected
     * prediction type.
     *
     * @param modelConfig the model config to validate
     * @param expectedPredictionType the required prediction type
     */
    private void validateModelConfig(
            ModelConfig modelConfig,
            String expectedPredictionType
    ) {
        if (modelConfig == null) {
            throw new IllegalArgumentException(
                    "ModelConfig cannot be null."
            );
        }

        String actualPredictionType =
                modelConfig.getPredictionType().toUpperCase();

        if (!actualPredictionType.equals(expectedPredictionType)) {
            throw new IllegalArgumentException(
                    "Expected prediction type "
                            + expectedPredictionType
                            + " but received "
                            + actualPredictionType
            );
        }
    }
}