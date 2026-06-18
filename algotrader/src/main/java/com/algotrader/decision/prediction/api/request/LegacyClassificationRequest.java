package com.algotrader.decision.prediction.api.request;

/**
 * Legacy feature-matrix request payload used by classification endpoints.
 */
public record LegacyClassificationRequest(
        /** Ticker symbol associated with the feature matrix. */
        String ticker,

        /** Feature matrix derived from the market data batch. */
        double[][] data
) {
}
