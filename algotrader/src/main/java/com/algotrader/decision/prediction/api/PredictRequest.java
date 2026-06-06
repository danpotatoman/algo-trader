package com.algotrader.decision.prediction.api;

/**
 * Request payload sent to a prediction endpoint.
 * 
 * <p>This DTO is intentionally model-agnostic. It does not contain feature
 * names, forecast horizons, or model metadata, as those concerns are managed
 * by configuration and the prediction endpoint itself.
 * 
 * <p>This record represents the JSON request body sent from the Java trading
 * system to a Python model service. It serves as a transport object between
 * the market data layer and the prediction API layer.
 *
 * <p>The request contains the ticker symbol associated with the prediction
 * and a two-dimensional feature matrix representing the model input.
 *
 * <p>The feature matrix is expected to have shape:
 *
 * <pre>
 * [timesteps][features]
 * </pre>
 *
 * where each row represents a single observation in time order and each
 * column represents a model feature.
 *
 * <p>The specific meaning, ordering, and number of features are determined
 * by the model configuration and must match the expectations of the target
 * prediction endpoint.
 */
public record PredictRequest(

        /** Ticker symbol associated with the prediction request. */
        String ticker,

        /**
         * Feature matrix used for prediction.
         */
        double[][] data

) {}