package com.algotrader.runtime;

/**
 * Exception thrown when a trading session cannot be initialized or completed.
 */
public final class TradingSessionException extends RuntimeException {

    public TradingSessionException(String message) {
        super(message);
    }

    public TradingSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
