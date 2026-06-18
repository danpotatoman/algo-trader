package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.marketdata.model.DataBatch;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Prediction provider that converts Python classification endpoint responses
 * into {@link ClassificationPrediction} domain objects.
 */
public final class PythonClassificationPredictionProvider
        implements PredictionProvider<ClassificationPrediction> {

    private final PythonPredictionClient client;

    public PythonClassificationPredictionProvider(
            PythonPredictionClient client
    ) {
        if (client == null) {
            throw new IllegalArgumentException(
                    "PythonPredictionClient cannot be null."
            );
        }

        this.client = client;
    }

    /**
     * Requests a classification prediction for the supplied market data batch.
     *
     * @param batch market data batch to send to the endpoint
     * @return classification prediction for the batch
     * @throws IllegalArgumentException if {@code batch} is null
     * @throws PredictionProviderException if the endpoint response is invalid
     */
    @Override
    public ClassificationPrediction makePrediction(
            DataBatch batch
    ) throws PredictionProviderException {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        JsonNode response = client.predict(batch);

        String ticker = response.get("ticker").asText();

        if (!batch.getTicker().equalsIgnoreCase(ticker)) {
            throw new PredictionProviderException(
                    "Classification response ticker does not match request ticker. "
                            + "Requested: " + batch.getTicker()
                            + ", received: " + ticker
            );
        }

        int prediction = response.get("prediction").asInt();
        double confidence = response.get("confidence").asDouble();
        int horizonMinutes = response.get("horizonMinutes").asInt();
        String label = response.get("label").asText();

        return new ClassificationPrediction(batch, ticker, prediction, confidence, horizonMinutes, label);
    }
}
