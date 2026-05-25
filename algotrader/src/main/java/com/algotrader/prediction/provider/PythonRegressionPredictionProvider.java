package com.algotrader.prediction.provider;

import com.algotrader.data.dataobjects.RegressionPrediction;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.prediction.api.PythonPredictionClient;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.prediction.RegressionPredictionProvider;

public final class PythonRegressionPredictionProvider
        implements RegressionPredictionProvider {

    private final PythonPredictionClient client;

    public PythonRegressionPredictionProvider(PythonPredictionClient client) {
        this.client = client;
    }

    @Override
    public RegressionPrediction makePrediction(DataBatch batch)
            throws PredictionProviderException {
        return client.requestRegressionPrediction(batch);
    }
}