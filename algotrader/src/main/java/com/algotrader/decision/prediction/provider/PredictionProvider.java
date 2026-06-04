package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ModelPrediction;
import com.algotrader.marketdata.model.DataBatch;

public interface PredictionProvider<T extends ModelPrediction> {

    T makePrediction(DataBatch batch) throws PredictionProviderException;
}