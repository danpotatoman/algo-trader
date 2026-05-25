package com.algotrader.prediction.provider;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.prediction.api.PythonPredictionClient;
import com.algotrader.prediction.ClassificationPredictionProvider;
import com.algotrader.prediction.PredictionProviderException;

public final class PythonClassificationPredictionProvider
        implements ClassificationPredictionProvider {

    private final PythonPredictionClient client;

    public PythonClassificationPredictionProvider(PythonPredictionClient client) {
        this.client = client;
    }

    @Override
    public ClassificationPrediction makePrediction(DataBatch batch)
            throws PredictionProviderException {
        return client.requestClassificationPrediction(batch);
    }
}