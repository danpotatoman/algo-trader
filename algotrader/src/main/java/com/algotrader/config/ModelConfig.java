package com.algotrader.config;

import java.util.List;

import com.algotrader.marketdata.model.TimeInterval;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable configuration describing a classification model available to the
 * trading system.
 *
 * <p>A {@code ModelConfig} defines the information required to invoke a
 * machine learning model, including its identifier, endpoint, expected
 * input structure, and prediction characteristics.
 *
 * <p>Model configurations are typically loaded from external JSON files
 * during application startup and used to construct prediction providers,
 * data pipelines, and trading sessions.
 *
 * <p>Examples of supported model metadata include:
 * <ul>
 *     <li>Model type (e.g. {@code CNN}, {@code LSTM})</li>
 *     <li>Prediction type (currently {@code CLASSIFICATION})</li>
 *     <li>Required input interval, batch size, and feature set</li>
 *     <li>Classification output semantics</li>
 * </ul>
 *
 * <p><b>TODO:</b> This configuration is currently designed around
 * classification models. Additional work is required to support
 * regression-specific output configuration and other prediction types
 * without overloading the existing structure.
 */
public final class ModelConfig {

    private final String modelId;
    private final String modelType;
    private final String predictionType;

    private final String endpoint;
    private final String description;
    private final String version;

    private final InputConfig input;
    private final OutputConfig output;

    /**
     * Creates a model configuration from deserialized JSON data.
     *
     * @param modelId unique identifier for the model
     * @param modelType model architecture or implementation type
     *        (e.g. {@code CNN}, {@code LSTM})
     * @param predictionType type of prediction produced by the model
     *        (e.g. {@code CLASSIFICATION})
     * @param endpoint endpoint used to request predictions from the model
     * @param description human-readable description of the model
     * @param version model version identifier
     * @param input input data requirements
     * @param output classification output characteristics
     * @throws IllegalArgumentException if any required field is invalid
     */
    @JsonCreator
    public ModelConfig(
            @JsonProperty("modelId") String modelId,
            @JsonProperty("modelType") String modelType,
            @JsonProperty("predictionType") String predictionType,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("description") String description,
            @JsonProperty("version") String version,
            @JsonProperty("input") InputConfig input,
            @JsonProperty("output") OutputConfig output
    ) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "Model ID cannot be null or blank."
            );
        }

        if (modelType == null || modelType.isBlank()) {
            throw new IllegalArgumentException(
                    "Model type cannot be null or blank."
            );
        }

        if (predictionType == null || predictionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Prediction type cannot be null or blank."
            );
        }

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException(
                    "Endpoint cannot be null or blank."
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

        if (input == null) {
            throw new IllegalArgumentException(
                    "InputConfig cannot be null."
            );
        }

        if (output == null) {
            throw new IllegalArgumentException(
                    "OutputConfig cannot be null."
            );
        }

        this.modelId = modelId;
        this.modelType = modelType;
        this.predictionType = predictionType;

        this.endpoint = endpoint;
        this.description = description;
        this.version = version;

        this.input = input;
        this.output = output;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelType() {
        return modelType;
    }

    public String getPredictionType() {
        return predictionType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public InputConfig getInput() {
        return input;
    }

    public OutputConfig getOutput() {
        return output;
    }

    /**
     * Configuration describing the market data required by a model.
     *
     * <p>This configuration specifies the candle interval, historical window
     * size, and feature set expected by the model when generating predictions.
     *
     * <p>The trading system uses this information to construct appropriately
     * sized {@code DataBatch} instances before invoking the model endpoint.
     */
    public static final class InputConfig {

        private final TimeInterval interval;
        private final int batchSize;
        private final List<String> features;

        @JsonCreator
        public InputConfig(
                @JsonProperty("interval") TimeInterval interval,
                @JsonProperty("batchSize") int batchSize,
                @JsonProperty("features") List<String> features
        ) {
            if (interval == null) {
                throw new IllegalArgumentException(
                        "Interval cannot be null."
                );
            }

            if (batchSize <= 0) {
                throw new IllegalArgumentException(
                        "Batch size must be positive."
                );
            }

            if (features == null || features.isEmpty()) {
                throw new IllegalArgumentException(
                        "Features cannot be null or empty."
                );
            }

            this.interval = interval;
            this.batchSize = batchSize;
            this.features = List.copyOf(features);
        }

        public TimeInterval getInterval() {
            return interval;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public List<String> getFeatures() {
            return features;
        }
    }

    /**
     * Configuration describing the outputs produced by a classification model.
     *
     * <p>The positive class label identifies the outcome represented by a
     * positive prediction, while the forecast horizon specifies how far into
     * the future the prediction applies.
     *
     * <p>For example, a model may predict whether a stock price will increase
     * within the next {@code horizonMinutes}.
     *
     * <p><b>TODO:</b> This configuration is classification-specific and should
     * eventually be generalized or replaced to support regression models and
     * other prediction types.
     */
    public static final class OutputConfig {

        private final String positiveClassLabel;
        private final int horizonMinutes;

        @JsonCreator
        public OutputConfig(
                @JsonProperty("positiveClassLabel") String positiveClassLabel,
                @JsonProperty("horizonMinutes") int horizonMinutes
        ) {
            if (positiveClassLabel == null
                    || positiveClassLabel.isBlank()) {
                throw new IllegalArgumentException(
                        "Positive class label cannot be null or blank."
                );
            }

            if (horizonMinutes <= 0) {
                throw new IllegalArgumentException(
                        "Horizon minutes must be positive."
                );
            }

            this.positiveClassLabel = positiveClassLabel;
            this.horizonMinutes = horizonMinutes;
        }

        public String getPositiveClassLabel() {
            return positiveClassLabel;
        }

        public int getHorizonMinutes() {
            return horizonMinutes;
        }
    }
}