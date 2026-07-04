package com.algotrader.logging;

public record SessionFailureLog(
        String exceptionType,
        String message
) {
    public static SessionFailureLog from(Exception exception) {
        if (exception == null) {
            return null;
        }

        return new SessionFailureLog(
                exception.getClass().getName(),
                exception.getMessage()
        );
    }
}