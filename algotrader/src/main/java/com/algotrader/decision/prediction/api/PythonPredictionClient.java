package com.algotrader.decision.prediction.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin HTTP client for sending prediction requests to a Python model service.
 *
 * <p>This class is transport-focused. It serializes already-mapped request DTOs
 * as JSON, sends them to the configured endpoint, and returns the raw JSON
 * response.
 *
 * <p>This client intentionally does not know about market data, request mapping,
 * or prediction semantics. Prediction-specific request mapping and response
 * parsing should be handled by higher-level prediction provider classes.
 */
public final class PythonPredictionClient {

    private final URI endpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Creates a prediction client for a single prediction endpoint.
     *
     * @param endpoint HTTP endpoint used to request predictions
     * @throws IllegalArgumentException if {@code endpoint} is null or not absolute
     */
    public PythonPredictionClient(URI endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint URI cannot be null.");
        }

        if (!endpoint.isAbsolute()) {
            throw new IllegalArgumentException("Endpoint URI must be absolute.");
        }

        this.endpoint = endpoint;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends an already-mapped prediction request DTO to the configured Python
     * endpoint.
     *
     * @param requestBody endpoint-specific request DTO
     * @return raw JSON response returned by the prediction endpoint
     * @throws IllegalArgumentException if {@code requestBody} is null
     * @throws PredictionProviderException if request serialization, HTTP
     *         communication, non-success responses, or response parsing fails
     */
    public JsonNode post(Object requestBody)
            throws PredictionProviderException {
        if (requestBody == null) {
            throw new IllegalArgumentException("requestBody cannot be null.");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint)
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

    public URI getEndpoint() {
        return endpoint;
    }
}