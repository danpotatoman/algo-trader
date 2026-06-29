package com.algotrader.decision.prediction.provider;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.PredictionType;
import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.ClassificationWithVolatilityRequestMapper;
import com.algotrader.decision.prediction.api.request.LegacyClassificationRequestMapper;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link PredictionProvider} instances from
 * trading configuration.
 *
 * <p>This factory is responsible for selecting and creating the appropriate
 * prediction provider implementation based on an endpoint's configured
 * prediction type.
 *
 * <p>Prediction providers act as the bridge between the trading system and
 * prediction endpoints, converting market data into
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
     * <p>The referenced endpoint configuration must declare a prediction type of
     * {@code CLASSIFICATION}.
     *
     * @param tradingPlan resolved trading plan containing the endpoint
     *        configuration
     * @return a classification prediction provider
     * @throws IllegalArgumentException if the trading plan references an endpoint
     *         that is not configured for classification predictions
     */
    public PredictionProvider<DataBatch, ClassificationPrediction> createClassificationProvider(
            ResolvedTradingPlan tradingPlan
        ) {

        if (tradingPlan == null) {
                throw new IllegalArgumentException("ResolvedTradingPlan cannot be null.");
        }

        EndpointConfig endpointConfig = tradingPlan.getEndpointConfig();

        validateEndpointConfig(
                endpointConfig,
                PredictionType.CLASSIFICATION
        );

        PythonPredictionClient client =
                new PythonPredictionClient(
                        endpointConfig.getEndpoint()
                );

        LegacyClassificationRequestMapper requestMapper =
                new LegacyClassificationRequestMapper();

        return new PythonClassificationPredictionProvider(client, requestMapper);
    }

    /**
     * Creates a classification-with-volatility prediction provider for the
     * supplied trading plan.
     *
     * <p>The referenced endpoint configuration must declare a prediction type
     * of {@code CLASSIFICATION_WITH_VOLATILITY}.
     *
     * @param tradingPlan resolved trading plan containing the endpoint
     *        configuration
     * @return a classification-with-volatility prediction provider
     * @throws IllegalArgumentException if the trading plan references an
     *         endpoint with a different prediction type
     */
    public PredictionProvider<DataBatch, ClassificationWithVolatilityPrediction> createClassificationWithVolatilityProvider(
            ResolvedTradingPlan tradingPlan
        ) {

        if (tradingPlan == null) {
                throw new IllegalArgumentException("ResolvedTradingPlan cannot be null.");
        }

        EndpointConfig endpointConfig = tradingPlan.getEndpointConfig();

        validateEndpointConfig(
                endpointConfig,
                PredictionType.CLASSIFICATION_WITH_VOLATILITY
        );

        PythonPredictionClient client =
                new PythonPredictionClient(
                        endpointConfig.getEndpoint()
                );

        ClassificationWithVolatilityRequestMapper requestMapper =
                new ClassificationWithVolatilityRequestMapper();

        return new PythonClassificationWithVolatilityPredictionProvider(client, requestMapper);
    }

    /**
     * Validates that an endpoint configuration declares the expected prediction
     * type.
     *
     * @param endpointConfig endpoint configuration to validate
     * @param expectedPredictionType required prediction type
     * @throws IllegalArgumentException if the configuration is null or declares
     *         a different prediction type
     */
    private void validateEndpointConfig(
        EndpointConfig endpointConfig,
        PredictionType expectedPredictionType
        ) {
                if (endpointConfig == null) {
                        throw new IllegalArgumentException(
                                "EndpointConfig cannot be null."
                        );
                }

                PredictionType actualPredictionType =
                        endpointConfig.getPredictionType();

                if (actualPredictionType != expectedPredictionType) {
                        throw new IllegalArgumentException(
                                "Expected prediction type "
                                        + expectedPredictionType
                                        + " but received "
                                        + actualPredictionType
                        );
                }
        }
}
