package com.algotrader.decision.prediction.api.request;

import java.util.List;

/**
 * Request payload sent to classification-with-volatility prediction endpoints.
 */
public record ClassificationWithVolatilityRequest(
        /** Ticker symbol associated with the market data rows. */
        String ticker,

        /** Ordered OHLCV rows used as endpoint input. */
        List<PredictionRowRequest> rows
) {
}
