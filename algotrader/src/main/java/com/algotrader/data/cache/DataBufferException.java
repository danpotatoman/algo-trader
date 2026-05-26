package com.algotrader.data.cache;

/**
 * Exception thrown when an error occurs during data buffering operations.
 *
 * <p>This exception is used to signal issues related to retrieving, loading,
 * or processing OHLCV data within {@link DataBuffer} implementations.
 *
 * <p>Typical causes include:
 * <ul>
 *     <li>Invalid request parameters (e.g. null ticker, invalid batch size)</li>
 *     <li>Missing or unreadable CSV files</li>
 *     <li>Malformed or invalid data within a CSV file</li>
 *     <li>Insufficient data to fulfill a batch request</li>
 * </ul>
 */
public class DataBufferException extends Exception {

    /**
     * Constructs a new {@code DataBufferException} with the specified detail message.
     *
     * @param message a descriptive message explaining the cause of the exception
     */
    public DataBufferException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DataBufferException} with the specified detail message
     * and underlying cause.
     *
     * @param message a descriptive message explaining the cause of the exception
     * @param cause the underlying exception that triggered this error
     */
    public DataBufferException(String message, Throwable cause) {
        super(message, cause);
    }
}