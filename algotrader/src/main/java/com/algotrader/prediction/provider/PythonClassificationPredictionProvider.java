package com.algotrader.prediction.provider;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.ClassificationScore;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.prediction.api.PythonPredictionClient;
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