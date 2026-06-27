package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Represents a portfolio action selected by the trading system during a
 * cycle.
 *
 * <p>In backtesting and paper trading, portfolio actions are applied directly
 * to {@code PortfolioState}. They do not necessarily represent confirmed
 * real-world broker executions.
 */
public record PortfolioAction(
            String ticker,       
            Action action,
            Instant executionTime,
            double price,
            double quantity
) {
    /**
     * Creates a portfolio action.
     *
     * @param ticker target of action
     * @param action executed action, such as {@code BUY} or {@code SELL}
     * @param executionTime execution timestamp
     * @param price execution price
     * @param quantity executed quantity
     * @throws IllegalArgumentException if any argument is invalid
     */
    public PortfolioAction(
            String ticker,
            Action action,
            Instant executionTime,
            double price,
            double quantity
    ) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "Action cannot be null."
            );
        }

        if (executionTime == null) {
            throw new IllegalArgumentException(
                    "Execution timestamp cannot be null."
            );
        }

        if (price < 0.0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative."
            );
        }

        if (quantity < 0.0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative."
            );
        }

        this.ticker = ticker;
        this.action = action;
        this.executionTime = executionTime;
        this.price = price;
        this.quantity = quantity;
    }

    public Action getAction() {
        return action;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }
}
