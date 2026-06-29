package com.algotrader.decision.prediction.api.request;

import com.algotrader.marketdata.model.DataBatch;

/**
 * Maps market data batches to the legacy feature-matrix request shape used by
 * classification endpoints.
 */
public final class LegacyClassificationRequestMapper
        implements PredictionRequestMapper<DataBatch, LegacyClassificationRequest> {

    @Override
    public LegacyClassificationRequest map(DataBatch batch) {
        return new LegacyClassificationRequest(
                batch.getTicker(),
                batch.toFeatureArray()
        );
    }
}