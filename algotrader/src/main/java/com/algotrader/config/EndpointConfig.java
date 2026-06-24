package com.algotrader.config;

import java.net.URI;
import java.util.Map;

import com.algotrader.marketdata.model.TimeInterval;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable configuration describing a prediction endpoint available to the
 * trading system.
 *
 * <p>An {@code EndpointConfig} defines the information required to invoke an
 * external prediction service, including:
 * <ul>
 *     <li>A unique endpoint identifier</li>
 *     <li>The prediction contract returned by the endpoint</li>
 *     <li>The endpoint URI used for inference requests</li>
 *     <li>The required input candle interval and number of historical candles</li>
 *     <li>Output statistics used to interpret or scale predictions</li>
 *     <li>Optional metadata describing the endpoint</li>
 * </ul>
 *
 * <p>The underlying implementation details of the endpoint are intentionally
 * hidden from the Java application. An endpoint may internally use a single
 * model, multiple models, ensembles, feature engineering pipelines, or any
 * other inference workflow.
 *
 * <p>Instances are typically loaded from external JSON configuration files
 * during application startup.
 */
public final class EndpointConfig {

    private final String endpointId;

    private final PredictionType predictionType;

    private final URI endpoint;

    private final String description;

    private final String version;

    private final TimeInterval interval;

    private final int numCandles;

    private final Map<String, Double> outputStatistics;

    /**
     * Creates an endpoint configuration from deserialized JSON data.
     *
     * @param endpointId unique identifier for the endpoint
     * @param predictionType prediction contract returned by the endpoint
     * @param endpoint URI used to request predictions
     * @param description human-readable description of the endpoint
     * @param version endpoint version identifier
     * @param interval candle interval required by the endpoint
     * @param numCandles number of historical candles required as input
     * @param outputStatistics named statistics used by downstream trading
     *        planners
     */
    @JsonCreator
    public EndpointConfig(
            @JsonProperty("endpointId") String endpointId,
            @JsonProperty("predictionType")
                    PredictionType predictionType,
            @JsonProperty("endpoint") URI endpoint,
            @JsonProperty("description") String description,
            @JsonProperty("version") String version,
            @JsonProperty("interval") TimeInterval interval,
            @JsonProperty("numCandles") int numCandles,
            @JsonProperty("outputStatistics")
                    Map<String, Double> outputStatistics
    ) {
        if (endpointId == null || endpointId.isBlank()) {
            throw new IllegalArgumentException(
                    "Endpoint ID cannot be null or blank."
            );
        }

        if (predictionType == null) {
            throw new IllegalArgumentException(
                    "Prediction type cannot be null."
            );
        }

        if (endpoint == null) {
            throw new IllegalArgumentException(
                    "Endpoint URI cannot be null."
            );
        }

        if (!endpoint.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Endpoint URI must be absolute."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Description cannot be null or blank."
            );
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Version cannot be null or blank."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "Interval cannot be null."
            );
        }

        if (numCandles <= 0) {
            throw new IllegalArgumentException(
                    "Number of candles must be positive."
            );
        }

        if (outputStatistics == null) {
            throw new IllegalArgumentException(
                    "Output statistics cannot be null."
            );
        }

        this.endpointId = endpointId;
        this.predictionType = predictionType;
        this.endpoint = endpoint;
        this.description = description;
        this.version = version;
        this.interval = interval;
        this.numCandles = numCandles;
        this.outputStatistics = Map.copyOf(outputStatistics);
    }

    public String getEndpointId() {
        return endpointId;
    }

    public PredictionType getPredictionType() {
        return predictionType;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public TimeInterval getInterval() {
        return interval;
    }

    public int getNumCandles() {
        return numCandles;
    }

    public Map<String, Double> getOutputStatistics() {
        return Map.copyOf(outputStatistics);
    }

    /**
     * Returns a configured output statistic required by a downstream planner.
     *
     * @param key statistic name
     * @return configured statistic value
     * @throws IllegalArgumentException if the statistic is not configured
     */
    public double getRequiredOutputStatistic(String key) {
        Double value = outputStatistics.get(key);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing output statistic: " + key
            );
        }

        return value;
    }
}
