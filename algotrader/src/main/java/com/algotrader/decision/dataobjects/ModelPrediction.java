package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Common contract implemented by all model prediction types.
 *
 * <p>A {@code ModelPrediction} represents the output of a machine learning
 * model for a specific market instrument and point in time. Implementations
 * may represent different prediction styles, such as classification or
 * regression forecasts.
 *
 * <p>The trading system uses this interface to access prediction metadata
 * without needing to know the specific prediction type.
 */
public interface ModelPrediction {

    /**
     * Returns a concise human-readable description of the prediction suitable
     * for logging and diagnostic output.
     *
     * @return a prediction summary
     */
    String summary();
    String getTicker();
    Instant getFinalTimestamp();
    TimeInterval getInterval();
}