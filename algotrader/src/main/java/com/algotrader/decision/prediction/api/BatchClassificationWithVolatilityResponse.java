package com.algotrader.decision.prediction.api;

import java.util.List;

/**
 * Response payload returned by a batch classification-with-volatility endpoint.
 *
 * <p>This record represents the Python model response before it is converted
 * into a collection of {@code ClassificationWithVolatilityPrediction}s.
 *
 * <p>Expected JSON format:
 *
 * <pre>
 * {
 *   "predictions": [
 *     {
 *       "ticker": "AAPL",
 *       "probability": 0.73,
 *       "volatility": 0.0045,
 *       "horizonMinutes": 30
 *     },
 *     {
 *       "ticker": "MSFT",
 *       "probability": 0.68,
 *       "volatility": 0.0038,
 *       "horizonMinutes": 30
 *     }
 *   ]
 * }
 * </pre>
 */
public record BatchClassificationWithVolatilityResponse(
        List<ClassificationWithVolatilityResponse> predictions
) {
    public BatchClassificationWithVolatilityResponse {
        if (predictions == null) {
            throw new IllegalArgumentException(
                    "predictions cannot be null."
            );
        }

        predictions = List.copyOf(predictions);
    }
}