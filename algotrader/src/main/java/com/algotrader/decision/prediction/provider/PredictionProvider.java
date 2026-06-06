package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ModelPrediction;
import com.algotrader.marketdata.model.DataBatch;

/**
 * Produces model predictions from market data.
 *
 * <p>A {@code PredictionProvider} encapsulates the logic required to convert
 * a {@link DataBatch} into a prediction produced by a machine learning model.
 * Implementations may obtain predictions from local models, remote services,
 * mock providers, or other prediction sources.
 *
 * <p>The prediction type is represented by the generic parameter
 * {@code T}, allowing implementations to return different prediction
 * representations such as classification or regression predictions.
 *
 * @param <T> the prediction type produced by this provider
 */
public interface PredictionProvider<T extends ModelPrediction> {

    /**
     * Generates a prediction for the supplied market data batch.
     *
     * @param batch market data used as model input
     * @return the resulting prediction
     * @throws IllegalArgumentException if {@code batch} is invalid
     * @throws PredictionProviderException if prediction generation fails
     */
    T makePrediction(DataBatch batch) throws PredictionProviderException;
}