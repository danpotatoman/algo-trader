package com.algotrader.config;

/**
 * Enumerates the supported trade generator types.
 *
 * <p>A {@code TradeGeneratorType} identifies the logic used to convert model
 * predictions into trade instructions.
 *
 * <p>Each type is associated with a corresponding trade generator pipeline
 * and may require additional parameters defined in a
 * {@link TradeGeneratorConfig}.
 *
 * <p><b>TODO:</b> Threshold classification and classification with volatility
 * are both threshold strategies and should probably be combined.
 */
public enum TradeGeneratorType {

    /**
     * Generates trades by comparing model predictions against a threshold.
     */
    THRESHOLD,

    /**
     * Generates trades using a directional classification threshold, with
     * volatility used to determine position sizing.
     */
    VOLATILITY_SCALED_THRESHOLD
}
