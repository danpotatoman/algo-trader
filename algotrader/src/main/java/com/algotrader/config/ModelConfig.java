package com.algotrader.config;

import java.util.List;

import com.algotrader.data.TimeInterval;

/**
 * Immutable configuration describing a prediction model.
 *
 * <p>A {@code ModelConfig} defines:
 * <ul>
 *     <li>Model metadata and identifiers</li>
 *     <li>Model endpoint and versioning information</li>
 *     <li>Expected input structure</li>
 *     <li>Prediction output characteristics</li>
 * </ul>
 *
 * <p>This class is intended to be loaded from external configuration
 * files such as JSON.
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
     * Constructs a {@code ModelConfig}.
     */
    public ModelConfig(
            String modelId,
            String modelType,
            String predictionType,
            String endpoint,
            String description,
            String version,
            InputConfig input,
            OutputConfig output
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
     * Immutable input configuration for a model.
     */
    public static final class InputConfig {

        private final TimeInterval interval;
        private final int batchSize;
        private final List<String> features;

        public InputConfig(
                TimeInterval interval,
                int batchSize,
                List<String> features
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
     * Immutable output configuration for a model.
     */
    public static final class OutputConfig {

        private final String positiveClassLabel;
        private final int horizonMinutes;

        public OutputConfig(
                String positiveClassLabel,
                int horizonMinutes
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