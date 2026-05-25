package com.algotrader.prediction;

import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.ModelPrediction;

public interface PredictionProvider<T extends ModelPrediction> {

    T makePrediction(DataBatch batch) throws PredictionProviderException;
}