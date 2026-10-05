package com.algotrader.decision.prediction.provider;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Produces model predictions from model inputs.
 *
 * <p>A {@code PredictionProvider} encapsulates the logic required to convert
 * an input object into a prediction produced by a machine learning model.
 * Implementations may obtain predictions from local models, remote services,
 * mock providers, or other prediction sources.
 *
 * <p>The input type and prediction type are represented by the generic
 * parameters {@code I} and {@code O}, allowing implementations to support
 * different prediction workflows such as single-batch and batch inference.
 *
 * @param <I> input type accepted by the provider
 * @param <O> prediction type produced by the provider
 */
public interface PredictionProvider<I, O> {

    default com.fasterxml.jackson.databind.JsonNode provenance() throws PredictionProviderException {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    }

    /**
     * Generates a prediction for the supplied model input.
     *
     * @param input model input
     * @return the resulting prediction
     * @throws IllegalArgumentException if {@code input} is invalid
     * @throws PredictionProviderException if prediction generation fails
     */
    O predict(I input) throws PredictionProviderException;

    /**
     * Returns the market data interval expected by this prediction provider.
     *
     * @return expected input interval
     */
    public TimeInterval getInterval();
}
