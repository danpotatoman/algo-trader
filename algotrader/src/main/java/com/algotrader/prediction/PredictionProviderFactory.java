package com.algotrader.prediction;

import com.algotrader.prediction.api.PythonPredictionClient;
import com.algotrader.prediction.provider.PythonClassificationPredictionProvider;
import com.algotrader.prediction.provider.PythonRegressionPredictionProvider;

public final class PredictionProviderFactory {

    private static final String BASE_URL = "http://127.0.0.1:8000";

    private static final String REGRESSION_ENDPOINT =
            "/predict/regression";

    private static final String CLASSIFICATION_ENDPOINT =
            "/predict/classification";

    private static final PythonPredictionClient CLIENT =
            new PythonPredictionClient(
                    BASE_URL,
                    REGRESSION_ENDPOINT,
                    CLASSIFICATION_ENDPOINT
            );

    private static final RegressionPredictionProvider REGRESSION_PROVIDER =
            new PythonRegressionPredictionProvider(CLIENT);

    private static final ClassificationPredictionProvider CLASSIFICATION_PROVIDER =
            new PythonClassificationPredictionProvider(CLIENT);

    private PredictionProviderFactory() {}

    public static RegressionPredictionProvider getRegressionProvider() {
        return REGRESSION_PROVIDER;
    }

    public static ClassificationPredictionProvider getClassificationProvider() {
        return CLASSIFICATION_PROVIDER;
    }
}