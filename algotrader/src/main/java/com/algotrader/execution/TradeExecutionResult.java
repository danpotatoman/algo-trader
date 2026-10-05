package com.algotrader.execution;

import java.time.Instant;

import com.algotrader.decision.dataobjects.Action;

/**
 * Result produced after executing or simulating a trade instruction.
 *
 * @param ticker executed ticker
 * @param action executed side
 * @param executionTime execution timestamp
 * @param price execution price
 * @param quantity executed quantity
 * @param cashAmount cash exchanged during execution
 */
public record TradeExecutionResult(
        String ticker,
        Action action,
        Instant executionTime,
        double price,
        double quantity,
        double cashAmount,
        String tradeId
) {

    public TradeExecutionResult(String ticker, Action action, Instant executionTime,
                                double price, double quantity, double cashAmount) {
        this(ticker, action, executionTime, price, quantity, cashAmount, null);
    }

    public TradeExecutionResult withTradeId(String id) {
        return new TradeExecutionResult(ticker, action, executionTime, price, quantity, cashAmount, id);
    }

    /**
     * Creates a trade execution result.
     *
     * @param ticker target of action
     * @param action executed action, such as {@code BUY} or {@code SELL}
     * @param executionTime execution timestamp
     * @param price execution price
     * @param quantity executed quantity
     * @param cashAmount cash exchanged during the execution
     * @throws IllegalArgumentException if any argument is invalid
     */
    public TradeExecutionResult {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be null or blank.");
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "Action cannot be null.");
        }

        if (executionTime == null) {
            throw new IllegalArgumentException(
                    "Execution timestamp cannot be null.");
        }

        if (price < 0.0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative.");
        }

        if (quantity < 0.0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative.");
        }

        if (cashAmount < 0.0) {
            throw new IllegalArgumentException(
                    "Cash amount cannot be negative.");
        }
    }
}
