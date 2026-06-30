package com.algotrader.decision.prediction.api.request;

import java.util.List;

import com.algotrader.marketdata.model.DataBatch;

/**
 * Maps multiple market data batches to the request shape expected by
 * batch classification-with-volatility endpoints.
 */
public final class BatchClassificationVolatilityRequestMapper
        implements PredictionRequestMapper<
                List<DataBatch>,
                BatchClassificationVolatilityRequest> {

    private final PredictionRequestMapper<
            DataBatch,
            ClassificationWithVolatilityRequest> requestMapper;

    public BatchClassificationVolatilityRequestMapper(
            PredictionRequestMapper<
                    DataBatch,
                    ClassificationWithVolatilityRequest> requestMapper
    ) {
        this.requestMapper = requestMapper;
    }

    @Override
    public BatchClassificationVolatilityRequest map(
            List<DataBatch> batches
    ) {
        return new BatchClassificationVolatilityRequest(
                batches.stream()
                        .map(requestMapper::map)
                        .toList()
        );
    }
}