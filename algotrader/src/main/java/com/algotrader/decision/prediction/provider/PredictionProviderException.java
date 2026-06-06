package com.algotrader.decision.prediction.provider;

/**
 * Exception indicating that a {@link PredictionProvider} was unable to
 * generate a prediction.
 *
 * <p>This exception serves as the primary checked exception for the
 * prediction layer, allowing prediction-related failures to be reported
 * without exposing implementation-specific exceptions to higher layers
 * of the application.
 *
 * <p>Typical causes include:
 * <ul>
 *     <li>Communication failures with external model services</li>
 *     <li>Invalid or malformed prediction responses</li>
 *     <li>Model endpoint errors</li>
 *     <li>Serialization or deserialization failures</li>
 *     <li>Prediction validation failures</li>
 * </ul>
 */
public class PredictionProviderException extends Exception {

    /**
     * Creates a prediction provider exception with the specified detail message.
     *
     * @param message the detail message
     */
    public PredictionProviderException(String message) {
        super(message);
    }

    /**
     * Creates a prediction provider exception with the specified detail message
     * and underlying cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public PredictionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a prediction provider exception with the specified underlying cause.
     *
     * @param cause the underlying cause
     */
    public PredictionProviderException(Throwable cause) {
        super(cause);
    }
}