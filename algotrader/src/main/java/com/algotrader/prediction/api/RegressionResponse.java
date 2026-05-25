package com.algotrader.prediction.api;

/**
 * Response payload returned by the Python regression model.
 *
 * <p>This object is deserialized from JSON returned by the
 * regression prediction endpoint.
 *
 * <p>Expected JSON format:
 *
 * <pre>
 * {
 *   "ticker": "AAPL",
 *   "return5m": 0.0012,
 *   "return10m": 0.0027,
 *   "return30m": 0.0041,
 *   "volatility30m": 0.0065
 * }
 * </pre>
 */
public record RegressionResponse(

        /**
         * Ticker symbol associated with the prediction.
         */
        String ticker,

        /**
         * Predicted return over the next 5 minutes.
         */
        double return5m,

        /**
         * Predicted return over the next 10 minutes.
         */
        double return10m,

        /**
         * Predicted return over the next 30 minutes.
         */
        double return30m,

        /**
         * Predicted volatility over the next 30 minutes.
         */
        double volatility30m

) {}