package com.algotrader.prediction.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.prediction.PredictionProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin HTTP client for requesting predictions from a Python model endpoint.
 *
 * <p>This class is transport-focused. It serializes a {@link DataBatch},
 * sends it to one configured endpoint, and returns the raw JSON response.
 *
 * <p>Prediction-specific parsing should be handled by the prediction provider,
 * not by this client.
 */
public final class PythonPredictionClient {

    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

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
     * @param batch the market data batch to send
     * @return the raw JSON response body
     * @throws IllegalArgumentException if batch is null
     * @throws PredictionProviderException if serialization, request, or response
     *         parsing fails
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

    public String getEndpoint() {
        return endpoint;
    }
}