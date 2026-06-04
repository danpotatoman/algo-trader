package com.algotrader.decision.prediction.provider;

/**
 * Exception thrown when a prediction provider fails to generate
 * a prediction.
 *
 * <p>This may occur due to:
 * <ul>
 *     <li>HTTP/API communication failures</li>
 *     <li>Invalid or malformed responses</li>
 *     <li>Model server errors</li>
 *     <li>Serialization/deserialization issues</li>
 *     <li>Prediction validation failures</li>
 * </ul>
 */
public class PredictionProviderException extends Exception {

    /**
     * Constructs a {@code PredictionProviderException}
     * with the specified detail message.
     *
     * @param message the detail message
     */
    public PredictionProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code PredictionProviderException}
     * with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public PredictionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code PredictionProviderException}
     * with the specified cause.
     *
     * @param cause the underlying cause
     */
    public PredictionProviderException(Throwable cause) {
        super(cause);
    }
}