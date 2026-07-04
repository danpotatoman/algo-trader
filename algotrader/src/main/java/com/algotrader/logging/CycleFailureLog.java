package com.algotrader.logging;

public record CycleFailureLog(
        String exceptionType,
        String message
) {
    public static CycleFailureLog from(Exception exception) {
        if (exception == null) {
            return null;
        }

        return new CycleFailureLog(
                exception.getClass().getName(),
                exception.getMessage()
        );
    }
}