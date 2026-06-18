package com.algotrader.decision.prediction.api.request;

import com.algotrader.marketdata.model.DataBatch;

/**
 * Converts market data batches into endpoint-specific prediction request DTOs.
 *
 * @param <T> request DTO type produced by the mapper
 */
public interface PredictionRequestMapper<T> {

    /**
     * Maps a market data batch into the request shape expected by an endpoint.
     *
     * @param batch market data batch to map
     * @return endpoint request DTO
     */
    T map(DataBatch batch);
}
