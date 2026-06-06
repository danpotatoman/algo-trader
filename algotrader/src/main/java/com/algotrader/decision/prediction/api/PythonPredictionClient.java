package com.algotrader.decision.prediction.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.marketdata.model.DataBatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin HTTP client for sending prediction requests to a Python model service.
 *
 * <p>This class is transport-focused. It converts a {@link DataBatch} into a
 * {@link PredictRequest}, serializes it as JSON, sends it to one configured
 * prediction endpoint, and returns the raw JSON response.
 *
 * <p>This client intentionally does not interpret prediction semantics.
 * Classification-specific or regression-specific parsing should be handled
 * by higher-level prediction provider classes.
 */
public final class PythonPredictionClient {

    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a prediction client for a single model endpoint.
     *
     * @param endpoint HTTP endpoint used to request predictions
     * @throws IllegalArgumentException if {@code endpoint} is null or blank
     */
    public PythonPredictionClient(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException(
                    "Endpoint cannot be null or blank."
            );
        }

        this.endpoint = endpoint;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a prediction request to the configured Python endpoint.
     *
     * <p>The supplied batch is converted into a feature matrix using
     * {@link DataBatch#toFeatureArray()} before being serialized and sent.
     *
     * @param batch market data batch to send to the model service
     * @return raw JSON response returned by the prediction endpoint
     * @throws IllegalArgumentException if {@code batch} is null
     * @throws PredictionProviderException if request serialization, HTTP
     *         communication, non-success responses, or response parsing fails
     */
    public JsonNode predict(DataBatch batch)
            throws PredictionProviderException {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        try {
            PredictRequest requestBody = new PredictRequest(
                    batch.getTicker(),
                    batch.toFeatureArray()
            );

            String requestJson =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new PredictionProviderException(
                        "Prediction request failed with status code "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            return objectMapper.readTree(response.body());

        } catch (IOException e) {
            throw new PredictionProviderException(
                    "Failed to serialize request or deserialize prediction response.",
                    e
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new PredictionProviderException(
                    "Prediction request interrupted.",
                    e
            );
        }
    }

    /**
     * Returns the endpoint used by this client.
     *
     * @return prediction endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }
}