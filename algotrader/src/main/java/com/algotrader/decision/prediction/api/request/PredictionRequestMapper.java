package com.algotrader.decision.prediction.api.request;

/**
 * Maps an input object into the request DTO expected by a prediction endpoint.
 *
 * <p>This interface is intentionally generic over both the input type and the
 * request DTO type. Implementations may map a single market data batch, a
 * collection of batches, or any other prediction input into the endpoint's
 * expected request shape.
 *
 * @param <I> input type accepted by the mapper
 * @param <O> request DTO type produced by the mapper
 */
public interface PredictionRequestMapper<I, O> {

    /**
     * Maps the supplied input into the request DTO expected by a prediction
     * endpoint.
     *
     * @param input input object to map
     * @return endpoint request DTO
     */
    O map(I input);
}