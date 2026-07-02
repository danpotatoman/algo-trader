package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Instruction to sell a specified quantity of a ticker.
 *
 * @param ticker ticker to sell
 * @param quantity quantity to sell
 * @param executionTime timestamp at which the sale should execute
 */
public record SellInstruction(
        String ticker,
        double quantity,
        Instant executionTime
) implements TradeInstruction {

    public SellInstruction {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be blank.");
        }

        if (quantity <= 0.0) {
            throw new IllegalArgumentException(
                    "Quantity must be positive.");
        }

        if (executionTime == null) {
            throw new IllegalArgumentException(
                    "Execution time cannot be null.");
        }
    }
}
