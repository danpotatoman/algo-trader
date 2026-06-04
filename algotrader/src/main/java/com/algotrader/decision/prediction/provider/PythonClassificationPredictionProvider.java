package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.dataobjects.ClassificationScore;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.marketdata.model.DataBatch;
import com.fasterxml.jackson.databind.JsonNode;

public final class PythonClassificationPredictionProvider
        implements ClassificationPredictionProvider {

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

        String responseTicker = response.get("ticker").asText();

        if (!batch.getTicker().equalsIgnoreCase(responseTicker)) {
            throw new PredictionProviderException(
                    "Classification response ticker does not match request ticker. "
                            + "Requested: " + batch.getTicker()
                            + ", received: " + responseTicker
            );
        }

        ClassificationScore score = new ClassificationScore(
                responseTicker,
                response.get("prediction").asInt(),
                response.get("confidence").asDouble(),
                response.get("label").asText()
        );

        return new ClassificationPrediction(batch, score);
    }
}