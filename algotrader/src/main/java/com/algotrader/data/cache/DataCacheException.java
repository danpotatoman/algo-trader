package com.algotrader.data.cache;

/**
 * Exception thrown when an error occurs during data cache operations.
 *
 * <p>This exception is used to signal issues related to retrieving, loading,
 * or processing OHLCV data within market data cache implementations.
 *
 * <p>Typical causes include:
 * <ul>
 *     <li>Invalid request parameters (e.g. null ticker, invalid batch size)</li>
 *     <li>Missing or unreadable CSV files</li>
 *     <li>Malformed or invalid data within a CSV file</li>
 *     <li>Insufficient data to fulfill a batch request</li>
 * </ul>
 */
public class DataCacheException extends Exception {

    /**
     * Constructs a new {@code DataCacheException} with the specified detail message.
     *
     * @param message a descriptive message explaining the cause of the exception
     */
    public DataCacheException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DataCacheException} with the specified detail message
     * and underlying cause.
     *
     * @param message a descriptive message explaining the cause of the exception
     * @param cause the underlying exception that triggered this error
     */
    public DataCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
