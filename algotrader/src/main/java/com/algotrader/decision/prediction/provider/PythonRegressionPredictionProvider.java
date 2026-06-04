package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.RegressionForecast;
import com.algotrader.decision.dataobjects.RegressionPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.marketdata.model.DataBatch;
import com.fasterxml.jackson.databind.JsonNode;

public final class PythonRegressionPredictionProvider
        implements RegressionPredictionProvider {

    private final PythonPredictionClient client;

    public PythonRegressionPredictionProvider(PythonPredictionClient client) {
        if (client == null) {
            throw new IllegalArgumentException(
                    "PythonPredictionClient cannot be null."
            );
        }

        this.client = client;
    }

    @Override
    public RegressionPrediction makePrediction(DataBatch batch)
            throws PredictionProviderException {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        JsonNode response = client.predict(batch);

        String responseTicker = response.get("ticker").asText();

        if (!batch.getTicker().equalsIgnoreCase(responseTicker)) {
            throw new PredictionProviderException(
                    "Regression response ticker does not match request ticker. "
                            + "Requested: " + batch.getTicker()
                            + ", received: " + responseTicker
            );
        }

        RegressionForecast forecast = new RegressionForecast(
                response.get("return5m").asDouble(),
                response.get("return10m").asDouble(),
                response.get("return30m").asDouble(),
                response.get("volatility30m").asDouble()
        );

        return new RegressionPrediction(batch, forecast);
    }
}