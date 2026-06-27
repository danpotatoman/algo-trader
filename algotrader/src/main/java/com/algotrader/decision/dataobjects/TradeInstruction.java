package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Represents an instruction from the decision layer to execute a trade.
 *
 * <p>A {@code TradeInstruction} encapsulates:
 * <ul>
 *     <li>The ticker to trade</li>
 *     <li>The action to perform ({@link Action#BUY} or {@link Action#SELL})</li>
 *     <li>The quantity to trade</li>
 *     <li>The intended execution time</li>
 * </ul>
 *
 * <p>A trade instruction represents intent only. It does not contain an
 * execution price, since the price is determined by the execution layer at
 * the time the trade is carried out.
 *
 * <p>Trade instructions are typically derived from higher-level trading
 * decisions such as {@link RoundTripTrade} instances and are consumed by a
 * {@code TradeExecutor}, which converts them into executed portfolio actions.
 *
 * @param ticker ticker symbol to trade
 * @param action trading action to perform
 * @param quantity quantity to trade
 * @param executionTime intended time at which the trade should be executed
 */
public record TradeInstruction(
        String ticker,
        Action action,
        double quantity,
        Instant executionTime
) {

    public TradeInstruction {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        if (executionTime == null) {
            throw new IllegalArgumentException("Execution time cannot be null.");
        }
    }

    /**
     * Returns whether this instruction represents a BUY action.
     *
     * @return {@code true} if the action is {@link Action#BUY}
     */
    public boolean isBuy() {
        return action == Action.BUY;
    }

    /**
     * Returns whether this instruction represents a SELL action.
     *
     * @return {@code true} if the action is {@link Action#SELL}
     */
    public boolean isSell() {
        return action == Action.SELL;
    }
}