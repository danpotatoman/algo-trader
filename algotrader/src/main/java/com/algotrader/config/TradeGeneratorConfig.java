package com.algotrader.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable configuration describing how a TradeGenerator should convert
 * model predictions into trade recommendations.
 *
 * <p>A TradeGenerator is configured through an underlying trade generation
 * strategy and its associated parameters.
 *
 * <p>A TradeGenerator is responsible for converting model predictions into
 * trade recommendations according to a configurable decision-making strategy.
 *
 * <p>This configuration defines:
 * <ul>
 *     <li>A unique identifier for the trade generator</li>
 *     <li>The trade generation strategy implementation to use</li>
 *     <li>Human-readable metadata and version information</li>
 *     <li>Strategy-specific parameters</li>
 * </ul>
 *
 * <p>Instances are typically loaded from external JSON configuration files
 * during application startup.
 *
 * <p><b>Note:</b> The current configuration structure is designed around
 * confidence-threshold classification strategies and may need to be expanded
 * as additional trade generation approaches are introduced.
 */
public final class TradeGeneratorConfig {

    private final String strategyId;
    private final StrategyType strategyType;
    private final String description;
    private final String version;

    private final StrategyParameters parameters;

    /**
     * Creates a trade generator configuration from deserialized JSON data.
     *
     * @param strategyId unique identifier for the trade generator
     * @param strategyType type of trade generation strategy to use
     * @param description human-readable description of the configuration
     * @param version configuration version identifier
     * @param parameters strategy-specific parameters
     * @throws IllegalArgumentException if any required field is invalid
     */
    @JsonCreator
    public TradeGeneratorConfig(
            @JsonProperty("strategyId") String strategyId,
            @JsonProperty("strategyType") StrategyType strategyType,
            @JsonProperty("description") String description,
            @JsonProperty("version") String version,
            @JsonProperty("parameters") StrategyParameters parameters
    ) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        if (strategyType == null) {
            throw new IllegalArgumentException(
                    "Strategy type cannot be null."
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

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "Parameters cannot be null."
            );
        }

        this.strategyId = strategyId;
        this.strategyType = strategyType;
        this.description = description;
        this.version = version;
        this.parameters = parameters;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Returns the strategy-specific parameters used when generating trades.
     *
     * @return the configured strategy parameters
     */
    public StrategyParameters getParameters() {
        return parameters;
    }

    /**
     * Configuration parameters for a confidence-based trade generation strategy.
     */
    public static final class StrategyParameters {

        private final Map<String, Object> values;

        @JsonCreator
        public StrategyParameters(
                Map<String, Object> values
        ) {
            if (values == null) {
                throw new IllegalArgumentException(
                        "Strategy parameter values cannot be null."
                );
            }

            this.values = Map.copyOf(values);
        }

        public double getRequiredDouble(String key) {
            Object value = getRequiredValue(key);

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException(
                        "Required strategy parameter must be numeric: " + key
                );
            }

            return number.doubleValue();
        }

        public int getRequiredInt(String key) {
            Object value = getRequiredValue(key);

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException(
                        "Required strategy parameter must be numeric: " + key
                );
            }

            return number.intValue();
        }

        public boolean has(String key) {
            return values.containsKey(key);
        }

        public Map<String, Object> asMap() {
            return values;
        }

        private Object getRequiredValue(String key) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "Strategy parameter key cannot be null or blank."
                );
            }

            if (!values.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Missing required strategy parameter: " + key
                );
            }

            return values.get(key);
        }
    }
}