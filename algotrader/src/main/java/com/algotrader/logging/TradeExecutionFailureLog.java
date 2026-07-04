package com.algotrader.logging;

import com.algotrader.execution.TradeExecutionException;

public record TradeExecutionFailureLog(
        String exceptionType,
        String message
) {
    public static TradeExecutionFailureLog from(TradeExecutionException exception) {
        if (exception == null) {
            return null;
        }

        return new TradeExecutionFailureLog(
                exception.getClass().getName(),
                exception.getMessage()
        );
    }
}