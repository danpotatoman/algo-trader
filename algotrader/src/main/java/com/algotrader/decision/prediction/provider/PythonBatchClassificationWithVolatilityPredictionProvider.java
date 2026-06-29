package com.algotrader.decision.prediction.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.BatchClassificationWithVolatilityRequest;
import com.algotrader.decision.prediction.api.request.PredictionRequestMapper;
import com.algotrader.marketdata.model.DataBatch;
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
    private final PredictionRequestMapper<List<DataBatch>, BatchClassificationWithVolatilityRequest> requestMapper;

    public PythonBatchClassificationWithVolatilityPredictionProvider(
            PythonPredictionClient client,
            PredictionRequestMapper<List<DataBatch>, BatchClassificationWithVolatilityRequest> requestMapper
    ) {
        if (client == null) {
            throw new IllegalArgumentException("PythonPredictionClient cannot be null.");
        }

        if (requestMapper == null) {
            throw new IllegalArgumentException("requestMapper cannot be null.");
        }

        this.client = client;
        this.requestMapper = requestMapper;
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

        Object requestBody = requestMapper.map(batches);
        JsonNode response = client.post(requestBody);

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

            DataBatch batch = batchesByTicker.get(ticker);

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