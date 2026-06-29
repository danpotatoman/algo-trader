package com.algotrader.decision.prediction.api;

/**
 * Response payload returned by a classification-with-volatility endpoint.
 *
 * <p>This record represents the Python model response before it is converted
 * into a {@code ClassificationWithVolatilityPrediction}.
 *
 * <p>Expected JSON format:
 *
 * <pre>
 * {
 *   "ticker": "AAPL",
 *   "probability": 0.73,
 *   "volatility": 0.0045,
 *   "horizonMinutes": 30
 * }
 * </pre>
 */
public record ClassificationWithVolatilityResponse(

        /** Ticker symbol associated with the prediction. */
        String ticker,

        /** Upward classification probability. */
        double probability,

        /** Non-negative forecast volatility. */
        double volatility,

        /** Prediction horizon in minutes. */
        int horizonMinutes
) {
}
