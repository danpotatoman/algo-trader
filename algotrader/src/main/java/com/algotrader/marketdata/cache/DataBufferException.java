package com.algotrader.marketdata.cache;

/**
 * Legacy exception associated with the original data-buffering subsystem.
 *
 * <p>This exception was used by the project's earlier market data pipeline
 * to report failures while loading, retrieving, or processing OHLCV data.
 *
 * <p>Typical causes included:
 * <ul>
 *     <li>Missing or unreadable CSV files</li>
 *     <li>Malformed market data</li>
 *     <li>Insufficient data to satisfy a request</li>
 *     <li>Failures during batch construction</li>
 * </ul>
 *
 * <p>The modern market data architecture uses
 * {@link DataCacheException} instead. This class remains only for
 * compatibility with legacy components.
 *
 * <p><b>TODO:</b> Remove this exception once all remaining references to the
 * original data-buffering subsystem have been eliminated.
 */
public class DataBufferException extends Exception {

    /**
     * Creates a data buffer exception with the specified detail message.
     *
     * @param message detail message describing the failure
     */
    public DataBufferException(String message) {
        super(message);
    }

    /**
     * Creates a data buffer exception with the specified detail message and
     * underlying cause.
     *
     * @param message detail message describing the failure
     * @param cause underlying cause of the failure
     */
    public DataBufferException(String message, Throwable cause) {
        super(message, cause);
    }
}