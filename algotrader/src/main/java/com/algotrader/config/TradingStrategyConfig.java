package com.algotrader.config;

/**
 * Immutable configuration describing a trading strategy.
 *
 * <p>A {@code TradingStrategyConfig} defines:
 * <ul>
 *     <li>Strategy identifiers and metadata</li>
 *     <li>Strategy type</li>
 *     <li>Strategy parameters</li>
 *     <li>Versioning information</li>
 * </ul>
 *
 * <p>This class is intended to be loaded from external configuration
 * files such as JSON.
 */
public final class TradingStrategyConfig {

    private final String strategyId;
    private final String strategyType;
    private final String description;
    private final String version;

    private final Parameters parameters;

    /**
     * Constructs a {@code TradingStrategyConfig}.
     */
    public TradingStrategyConfig(
            String strategyId,
            String strategyType,
            String description,
            String version,
            Parameters parameters
    ) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        if (strategyType == null || strategyType.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy type cannot be null or blank."
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

    /**
     * Returns the unique strategy identifier.
     *
     * @return the strategy ID
     */
    public String getStrategyId() {
        return strategyId;
    }

    /**
     * Returns the strategy type.
     *
     * @return the strategy type
     */
    public String getStrategyType() {
        return strategyType;
    }

    /**
     * Returns the strategy description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the config version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the strategy parameters.
     *
     * @return strategy parameters
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Immutable strategy parameter configuration.
     */
    public static final class Parameters {

        private final double confidenceThreshold;
        private final double buyQuantity;

        /**
         * Constructs strategy parameters.
         *
         * @param confidenceThreshold minimum confidence required to trade
         * @param buyQuantity quantity to trade when conditions are met
         */
        public Parameters(
                double confidenceThreshold,
                double buyQuantity
        ) {
            if (confidenceThreshold < 0.0
                    || confidenceThreshold > 1.0) {
                throw new IllegalArgumentException(
                        "Confidence threshold must be between 0 and 1."
                );
            }

            if (buyQuantity < 0.0) {
                throw new IllegalArgumentException(
                        "Buy quantity cannot be negative."
                );
            }

            this.confidenceThreshold = confidenceThreshold;
            this.buyQuantity = buyQuantity;
        }

        /**
         * Returns the minimum confidence required to trade.
         *
         * @return the confidence threshold
         */
        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        /**
         * Returns the trade quantity.
         *
         * @return the buy quantity
         */
        public double getBuyQuantity() {
            return buyQuantity;
        }
    }
}