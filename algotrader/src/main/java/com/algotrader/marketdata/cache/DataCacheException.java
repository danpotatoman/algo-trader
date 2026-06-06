package com.algotrader.marketdata.cache;

/**
 * Exception thrown when market data cannot be retrieved, loaded, or processed.
 *
 * <p>{@code DataCacheException} represents failures originating from the
 * market data layer, including cache lookups, data loading, data validation,
 * and underlying data source operations.
 *
 * <p>Typical causes include:
 * <ul>
 *     <li>Requested market data does not exist</li>
 *     <li>Insufficient historical data to satisfy a request</li>
 *     <li>Corrupted or invalid market data</li>
 *     <li>Failures in underlying data sources</li>
 *     <li>Cache population or retrieval failures</li>
 * </ul>
 *
 * <p>This exception provides a common error type for market data operations,
 * allowing higher-level trading components to handle data-access failures
 * without depending on specific storage implementations.
 */
public class DataCacheException extends Exception {

    /**
     * Creates a data cache exception with the specified detail message.
     *
     * @param message detail message describing the failure
     */
    public DataCacheException(String message) {
        super(message);
    }

    /**
     * Creates a data cache exception with the specified detail message and
     * underlying cause.
     *
     * @param message detail message describing the failure
     * @param cause underlying cause of the failure
     */
    public DataCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
