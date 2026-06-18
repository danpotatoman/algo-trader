package com.algotrader.config;

/**
 * Enumerates the supported trade generation strategies.
 *
 * <p>A {@code StrategyType} identifies the logic used to convert model
 * predictions into trade recommendations.
 *
 * <p>Each strategy type is associated with a corresponding
 * trade generator implementation and may require additional
 * strategy-specific parameters defined in a
 * {@link TradeGeneratorConfig}.
 *
 * <p><b>TODO:</b> Threshold classification and classification with volatility
 * are both threshold strategies and should probably be combined.
 */
public enum StrategyType {

    /**
     * Generates trades by comparing a prediction confidence score against
     * a configured threshold.
     *
     * <p>Typically used with binary classification models.
     */
    THRESHOLD_CLASSIFICATION,

    /**
     * Generates trades from regression forecasts using configurable thresholds
     * or expected return targets.
     *
     * <p>Reserved for future use; regression prediction types are not yet
     * supported end-to-end.
     */
    REGRESSION,

    /**
     * Generates trades using both directional classification confidence
     * and forecast volatility.
     *
     * <p>For example, a trade may only be generated when confidence exceeds
     * a minimum threshold and predicted volatility remains below a maximum
     * threshold.
     */
    CLASSIFICATION_WITH_VOLATILITY
}
