package com.algotrader.execution;

/**
 * Exception thrown when a trade instruction cannot be executed or simulated.
 */
public final class TradeExecutionException extends RuntimeException {

    public TradeExecutionException(String message) {
        super(message);
    }

    public TradeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
