package com.algotrader.prediction;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.DataBatch;

public interface ClassificationPredictionProvider
        extends PredictionProvider<ClassificationPrediction> {
    ClassificationPrediction makePrediction(DataBatch batch)
            throws PredictionProviderException;
}