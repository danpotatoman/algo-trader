package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.ClassificationWithVolatilityRequest;
import com.algotrader.decision.prediction.api.request.PredictionRequestMapper;
import com.algotrader.marketdata.model.DataBatch;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Prediction provider that converts Python classification-with-volatility
 * endpoint responses into {@link ClassificationWithVolatilityPrediction}
 * domain objects.
 */
public final class PythonClassificationWithVolatilityPredictionProvider
        implements PredictionProvider<DataBatch, ClassificationWithVolatilityPrediction> {

    private final PythonPredictionClient client;
    private final PredictionRequestMapper<DataBatch, ClassificationWithVolatilityRequest> requestMapper;

    public PythonClassificationWithVolatilityPredictionProvider(
            PythonPredictionClient client,
            PredictionRequestMapper<DataBatch, ClassificationWithVolatilityRequest> requestMapper
    ) {
        if (client == null) {
            throw new IllegalArgumentException(
                    "PythonPredictionClient cannot be null."
            );
        }

        if (requestMapper == null) {
            throw new IllegalArgumentException(
                    "PredictionRequestMapper cannot be null."
            );
        }

        this.client = client;
        this.requestMapper = requestMapper;
    }

    /**
     * Requests a classification-with-volatility prediction for a market data
     * batch.
     *
     * @param batch market data batch to send to the endpoint
     * @return classification-with-volatility prediction for the batch
     * @throws IllegalArgumentException if {@code batch} is null
     * @throws PredictionProviderException if the endpoint response is invalid
     */
    @Override
    public ClassificationWithVolatilityPrediction predict(
            DataBatch batch
    ) throws PredictionProviderException {

        
        if (batch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        Object requestBody = requestMapper.map(batch);
        JsonNode response = client.post(requestBody);

        validateResponse(response);

        double probability = response.get("probability").asDouble();
        double volatility = response.get("volatility").asDouble();
        int horizonMinutes = response.get("horizonMinutes").asInt();

        return new ClassificationWithVolatilityPrediction(
                batch,
                probability,
                volatility,
                horizonMinutes
        );
    }

    private static void validateResponse(//TODO: validate horizonMinutes too
            JsonNode response
    ) throws PredictionProviderException {
        if (response == null || response.isNull()) {
            throw new PredictionProviderException(
                    "Classification-with-volatility response cannot be null."
            );
        }

        if (!response.hasNonNull("probability")) {
            throw new PredictionProviderException(
                    "Classification-with-volatility response is missing probability."
            );
        }

        if (!response.hasNonNull("volatility")) {
            throw new PredictionProviderException(
                    "Classification-with-volatility response is missing volatility."
            );
        }

        double probability = response.get("probability").asDouble();
        double volatility = response.get("volatility").asDouble();

        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            throw new PredictionProviderException(
                    "probability must be finite."
            );
        }

        if (probability < 0.0 || probability > 1.0) {
            throw new PredictionProviderException(
                    "probability must be between 0.0 and 1.0."
            );
        }

        if (Double.isNaN(volatility) || Double.isInfinite(volatility)) {
            throw new PredictionProviderException(
                    "volatility must be finite."
            );
        }

        if (volatility < 0.0) {
            throw new PredictionProviderException(
                    "volatility must be non-negative."
            );
        }
    }
}
