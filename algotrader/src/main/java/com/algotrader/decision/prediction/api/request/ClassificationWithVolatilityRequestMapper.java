package com.algotrader.decision.prediction.api.request;

import com.algotrader.marketdata.model.DataBatch;

/**
 * Maps market data batches to the row-oriented request shape expected by
 * classification-with-volatility endpoints.
 */
public final class ClassificationWithVolatilityRequestMapper
        implements PredictionRequestMapper<DataBatch, ClassificationWithVolatilityRequest> {

    @Override
    public ClassificationWithVolatilityRequest map(DataBatch batch) {
        return new ClassificationWithVolatilityRequest(
                batch.getTicker(),
                batch.getRows().stream()
                        .map(PredictionRowRequest::from)
                        .toList()
        );
    }
}
