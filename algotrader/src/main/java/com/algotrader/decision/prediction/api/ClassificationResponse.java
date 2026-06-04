package com.algotrader.decision.prediction.api;

/**
 * Response payload returned by the Python classification model.
 *
 * <p>This object is deserialized from JSON returned by the
 * classification prediction endpoint.
 *
 * <p>Expected JSON format:
 *
 * <pre>
 * {
 *   "ticker": "AAPL",
 *   "prediction": 1,
 *   "confidence": 0.83,
 *   "label": "up"
 * }
 * </pre>
 */
public record ClassificationResponse(

        /**
         * Ticker symbol associated with the prediction.
         */
        String ticker,

        /**
         * Predicted class value.
         *
         * <p>Typically:
         * <ul>
         *     <li>1 = upward movement</li>
         *     <li>0 = downward/no movement</li>
         * </ul>
         */
        int prediction,

        /**
         * Model confidence for the prediction.
         *
         * <p>Expected range:
         * <pre>
         * [0.0, 1.0]
         * </pre>
         */
        double confidence,

        /**
         * Human-readable prediction label.
         *
         * <p>Examples:
         * <ul>
         *     <li>"up"</li>
         *     <li>"down"</li>
         * </ul>
         */
        String label

) {}