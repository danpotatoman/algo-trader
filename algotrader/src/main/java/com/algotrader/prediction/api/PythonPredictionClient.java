package com.algotrader.prediction.api;

import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.RegressionForecast;
import com.algotrader.data.dataobjects.RegressionPrediction;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.ClassificationScore;
import com.algotrader.prediction.PredictionProviderException;

public final class PythonPredictionClient {

    private final String baseUrl;
    private final String regressionEndpoint;
    private final String classificationEndpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PythonPredictionClient(
            String baseUrl,
            String regressionEndpoint,
            String classificationEndpoint
    ) {
        this.baseUrl = baseUrl;
        this.regressionEndpoint = regressionEndpoint;
        this.classificationEndpoint = classificationEndpoint;

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public RegressionPrediction requestRegressionPrediction(
                DataBatch batch
        ) throws PredictionProviderException {

        try {

                String url = baseUrl + regressionEndpoint;

                PredictRequest requestBody = new PredictRequest(
                        batch.getTicker(),
                        batch.toFeatureArray()
                );

                String requestJson =
                        objectMapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                throw new PredictionProviderException(
                        "Regression request failed with status code "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
                }

                RegressionResponse responseBody =
                        objectMapper.readValue(
                                response.body(),
                                RegressionResponse.class
                        );

                if (!batch.getTicker().equalsIgnoreCase(responseBody.ticker())) {
                throw new PredictionProviderException(
                        "Regression response ticker does not match request ticker. " +
                        "Requested: " + batch.getTicker() +
                        ", received: " + responseBody.ticker()
                );
                }

                RegressionForecast forecast = new RegressionForecast(
                        responseBody.return5m(),
                        responseBody.return10m(),
                        responseBody.return30m(),
                        responseBody.volatility30m()
                );

                return new RegressionPrediction(batch, forecast);

        } catch (IOException e) {

                throw new PredictionProviderException(
                        "Failed to serialize or deserialize regression prediction.",
                        e
                );

        } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new PredictionProviderException(
                        "Regression prediction request interrupted.",
                        e
                );
        }
        }

    public ClassificationPrediction requestClassificationPrediction(
            DataBatch batch
    ) throws PredictionProviderException {

        try {

            String url = baseUrl + classificationEndpoint;

            PredictRequest requestBody = new PredictRequest(
                    batch.getTicker(),
                    batch.toFeatureArray()
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new PredictionProviderException(
                        "Classification request failed with status code "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            ClassificationResponse responseBody =
            objectMapper.readValue(
                    response.body(),
                    ClassificationResponse.class
            );

        if (!batch.getTicker().equalsIgnoreCase(responseBody.ticker())) {
            throw new PredictionProviderException(
                    "Classification response ticker does not match request ticker. " +
                    "Requested: " + batch.getTicker() +
                    ", received: " + responseBody.ticker()
            );
        }

        ClassificationScore score = new ClassificationScore(
                responseBody.ticker(),
                responseBody.prediction(),
                responseBody.confidence(),
                responseBody.label()
        );

        return new ClassificationPrediction(batch, score);

        } catch (IOException e) {

            throw new PredictionProviderException(
                    "Failed to serialize or deserialize classification prediction.",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new PredictionProviderException(
                    "Classification prediction request interrupted.",
                    e
            );
        }
    }
}