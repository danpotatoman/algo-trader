package com.algotrader.config;


/**
 * Enumerates the prediction contracts supported by prediction endpoints.
 *
 * <p>The prediction type defines the structure of data returned by an
 * endpoint and determines which prediction providers, interpreters, and
 * trade generation strategies can consume the result.
 *
 * <p>Classification-based prediction types are fully supported throughout
 * the trading pipeline. Regression support is planned but has not yet been
 * implemented.
 */
public enum PredictionType {

    /**
     * Binary or multi-class classification output.
     *
     * <p>Examples include directional predictions such as whether a price
     * will exceed a specified threshold over a future time horizon.
     *
     * <p>This prediction type is fully supported by the trading system.
     */
    CLASSIFICATION,

    /**
     * Continuous numerical forecast output.
     *
     * <p>Examples include predicted returns, prices, or volatility values.
     *
     * <p><b>Note:</b> Regression support is not yet implemented throughout
     * the prediction and trade generation pipeline. This enum value exists
     * as a placeholder for future development.
     */
    REGRESSION,

    /**
     * Classification output augmented with additional volatility metrics.
     *
     * <p>This prediction type combines a directional classification signal
     * with one or more volatility forecasts to enable risk-aware trade
     * generation.
     *
     * <p>This prediction type is fully supported by the trading system.
     */
    CLASSIFICATION_WITH_VOLATILITY
}
