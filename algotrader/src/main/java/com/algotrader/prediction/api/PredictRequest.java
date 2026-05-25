package com.algotrader.prediction.api;

/**
 * Request payload sent to the Python prediction server.
 *
 * <p>This object is serialized into JSON and contains:
 * <ul>
 *     <li>The ticker symbol associated with the data</li>
 *     <li>The feature matrix used for prediction</li>
 * </ul>
 *
 * <p>The feature matrix is expected to have shape:
 *
 * <pre>
 * [timesteps][features]
 * </pre>
 *
 * where each row represents one candle/timestep and each
 * column represents one engineered feature.
 */
public record PredictRequest(

        /**
         * Ticker symbol associated with the prediction request.
         */
        String ticker,

        /**
         * Feature matrix used for prediction.
         */
        double[][] data

) {}