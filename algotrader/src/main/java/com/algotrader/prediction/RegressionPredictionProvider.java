package com.algotrader.prediction;

import com.algotrader.data.dataobjects.RegressionPrediction;
import com.algotrader.data.dataobjects.DataBatch;

public interface RegressionPredictionProvider
        extends PredictionProvider<RegressionPrediction> {
    RegressionPrediction makePrediction(DataBatch batch)
            throws PredictionProviderException;
}