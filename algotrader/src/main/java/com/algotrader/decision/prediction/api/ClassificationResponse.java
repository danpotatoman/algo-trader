package com.algotrader.decision.prediction.api;

/**
 * Response payload returned by a classification prediction endpoint.
 *
 * <p>This record represents the JSON response returned by the Python model
 * service and serves as a transport object between the HTTP API layer and
 * the application's prediction domain objects.
 *
 * <p>Instances are typically deserialized directly from endpoint responses
 * before being converted into {@code ClassificationScore} objects.
 *
 * <p>This DTO exists primarily to support the project's original
 * classification-based prediction pipeline. Newer models are expected to
 * favor regression outputs, but classification support is retained for
 * experimentation and backwards compatibility.
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

    /** Ticker symbol associated with the prediction. */
    String ticker,

    /**
     * Predicted class value.
     *
     * <p>Current convention:
     * <ul>
     *     <li>{@code 1} = price expected to increase by at least the model's
     *         configured threshold within the forecast horizon</li>
     *     <li>{@code 0} = any other outcome</li>
     * </ul>
     *
     * <p>The exact threshold and forecast horizon are model-dependent.
     */
    int prediction,

    /**
     * Model confidence associated with the predicted class.
     *
     * <p>Expected range: {@code [0.0, 1.0]}.
     */
    double confidence,

    /** Human-readable label describing the prediction. */
    String label

) {}