package com.algotrader.decision.prediction.api.request;

import java.util.List;

/**
 * Request DTO for batch classification-with-volatility prediction endpoints.
 *
 * @param batches endpoint-specific single-batch prediction requests
 */
public record BatchClassificationVolatilityRequest(
        List<ClassificationWithVolatilityRequest> batches
) {
    public BatchClassificationVolatilityRequest {
        if (batches == null) {
            throw new IllegalArgumentException("batches cannot be null.");
        }

        batches = List.copyOf(batches);
    }
}