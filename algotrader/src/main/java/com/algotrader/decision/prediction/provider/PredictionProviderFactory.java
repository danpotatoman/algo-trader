package com.algotrader.decision.prediction.provider;

import com.algotrader.config.ModelConfig;
import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.dataobjects.RegressionPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link PredictionProvider} instances from
 * trading configuration.
 *
 * <p>This factory is responsible for selecting and creating the appropriate
 * prediction provider implementation based on a model's configured
 * prediction type.
 *
 * <p>Prediction providers act as the bridge between the trading system and
 * machine learning models, converting market data into
 * {@link com.algotrader.decision.dataobjects.ModelPrediction} instances.
 *
 * <p>The current implementation creates providers backed by Python model
 * endpoints accessed through {@link PythonPredictionClient}.
 */
public final class PredictionProviderFactory {

    /**
     * Creates a prediction provider factory.
     */
    public PredictionProviderFactory() {}

   /**
     * Creates a classification prediction provider for the supplied trading plan.
     *
     * <p>The referenced model configuration must declare a prediction type of
     * {@code CLASSIFICATION}.
     *
     * @param tradingPlan resolved trading plan containing the model
     *        configuration
     * @return a classification prediction provider
     * @throws IllegalArgumentException if the trading plan references a model
     *         that is not configured for classification predictions
     */
    public PredictionProvider<ClassificationPrediction> createClassificationProvider(
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
     * Creates a regression prediction provider from the supplied model
     * configuration.
     *
     * <p>The supplied model configuration must declare a prediction type of
     * {@code REGRESSION}.
     *
     * @param modelConfig regression model configuration
     * @return a regression prediction provider
     * @throws IllegalArgumentException if the model is not configured for
     *         regression predictions
     */
    public PredictionProvider<RegressionPrediction> createRegressionProvider(
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
     * Validates that a model configuration declares the expected prediction
     * type.
     *
     * @param modelConfig model configuration to validate
     * @param expectedPredictionType required prediction type
     * @throws IllegalArgumentException if the configuration is null or declares
     *         a different prediction type
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