package com.algotrader.decision.prediction.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.BatchClassificationVolatilityRequest;
import com.algotrader.decision.prediction.api.request.PredictionRequestMapper;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Prediction provider that requests classification-with-volatility predictions
 * for multiple market data batches in one Python API call.
 */
public final class PythonBatchClassificationWithVolatilityPredictionProvider
        implements PredictionProvider<
                List<DataBatch>,
                List<ClassificationWithVolatilityPrediction>> {

    private final PythonPredictionClient client;
    private final PredictionRequestMapper<List<DataBatch>, BatchClassificationVolatilityRequest> requestMapper;
    private final TimeInterval interval;
    private JsonNode serviceProvenance;

    @Override
    public JsonNode provenance() throws PredictionProviderException {
        if (serviceProvenance == null) {
            JsonNode metadata = client.provenance();
            String id = metadata.path("serverInstanceId").asText();
            if (!id.matches("[0-9a-fA-F-]{36}")
                    || !client.getEndpoint().getPath().equals(metadata.path("batchEndpoint").asText())
                    || !metadata.path("models").isArray() || metadata.path("models").size() != 2) {
                throw new PredictionProviderException("Invalid prediction service provenance; restart the updated model server");
            }
            for (var model : metadata.path("models")) {
                if (!model.path("sha256").asText().matches("[0-9a-f]{64}")) {
                    throw new PredictionProviderException("Model provenance is missing a SHA-256 hash");
                }
            }
            serviceProvenance = metadata;
        }
        return serviceProvenance.deepCopy();
    }

    /**
     * Creates a batch classification-with-volatility prediction provider.
     *
     * @param client client used to call the Python prediction endpoint
     * @param requestMapper mapper from data batches to endpoint requests
     * @param interval market data interval expected by each input batch
     * @throws IllegalArgumentException if any argument is null
     */
    public PythonBatchClassificationWithVolatilityPredictionProvider(
        PythonPredictionClient client,
        PredictionRequestMapper<List<DataBatch>, BatchClassificationVolatilityRequest> requestMapper,
        TimeInterval interval
    ) {
        if (client == null) {
            throw new IllegalArgumentException("PythonPredictionClient cannot be null.");
        }

        if (requestMapper == null) {
            throw new IllegalArgumentException("requestMapper cannot be null.");
        }

        if (interval == null) {
            throw new IllegalArgumentException("interval cannot be null.");
        }

        this.client = client;
        this.requestMapper = requestMapper;
        this.interval = interval;
    }

    @Override
    public List<ClassificationWithVolatilityPrediction> predict(
            List<DataBatch> batches
    ) throws PredictionProviderException {
        if (batches == null) {
            throw new IllegalArgumentException("batches cannot be null.");
        }

        if (batches.isEmpty()) {
            return List.of();
        }

        validateIntervals(batches);
        String serverId = provenance().path("serverInstanceId").asText();

        Object requestBody = requestMapper.map(batches);
        JsonNode response = client.post(requestBody);
        if (!serverId.equals(response.path("serverInstanceId").asText())) {
            throw new PredictionProviderException("Prediction service changed during run; refusing mixed model provenance");
        }

        JsonNode predictionsNode = response.get("predictions");

        if (predictionsNode == null || !predictionsNode.isArray()) {
            throw new PredictionProviderException(
                    "Batch classification-with-volatility response must contain "
                            + "a predictions array."
            );
        }

        if (predictionsNode.size() != batches.size()) {
            throw new PredictionProviderException(
                    "Batch response size does not match request size. Requested: "
                            + batches.size()
                            + ", received: "
                            + predictionsNode.size()
            );
        }

        Map<String, DataBatch> batchesByTicker = new HashMap<>();

        for (DataBatch batch : batches) {
            String ticker = batch.getTicker().toUpperCase();

            if (batchesByTicker.put(ticker, batch) != null) {
                throw new PredictionProviderException(
                        "Duplicate ticker in batch prediction request: " + ticker
                );
            }
        }

        List<ClassificationWithVolatilityPrediction> predictions =
                new ArrayList<>();

        for (JsonNode predictionNode : predictionsNode) {
            String ticker = requiredText(predictionNode, "ticker").toUpperCase();

            DataBatch batch = batchesByTicker.remove(ticker);

            if (batch == null) {
                throw new PredictionProviderException(
                        "Received prediction for unexpected ticker: " + ticker
                );
            }

            double probability = requiredDouble(predictionNode, "probability");
            double volatility = requiredDouble(predictionNode, "volatility");
            int horizonMinutes = requiredInt(predictionNode, "horizonMinutes");

            predictions.add(
                    new ClassificationWithVolatilityPrediction(
                            batch,
                            probability,
                            volatility,
                            horizonMinutes
                    )
            );
        }

        return predictions;
    }

    /**
     * Returns the market data interval expected by this prediction provider.
     *
     * @return expected input interval
     */
    @Override
    public TimeInterval getInterval() {
        return interval;
    }

    private static String requiredText(JsonNode node, String fieldName)
            throws PredictionProviderException {
        JsonNode field = node.get(fieldName);

        if (field == null || !field.isTextual()) {
            throw new PredictionProviderException(
                    "Expected text field in prediction response: " + fieldName
            );
        }

        return field.asText();
    }

    private void validateIntervals(List<DataBatch> batches)
            throws PredictionProviderException {

        for (DataBatch batch : batches) {
            if (batch.getInterval() != interval) {
                throw new PredictionProviderException(
                        "Expected DataBatch interval "
                                + interval
                                + " but received "
                                + batch.getInterval()
                                + " for ticker "
                                + batch.getTicker()
                );
            }
        }
    }

    private static double requiredDouble(JsonNode node, String fieldName)
            throws PredictionProviderException {
        JsonNode field = node.get(fieldName);

        if (field == null || !field.isNumber()) {
            throw new PredictionProviderException(
                    "Expected numeric field in prediction response: " + fieldName
            );
        }

        return field.asDouble();
    }

    private static int requiredInt(JsonNode node, String fieldName)
            throws PredictionProviderException {
        JsonNode field = node.get(fieldName);

        if (field == null || !field.canConvertToInt()) {
            throw new PredictionProviderException(
                    "Expected integer field in prediction response: " + fieldName
            );
        }

        return field.asInt();
    }
}
