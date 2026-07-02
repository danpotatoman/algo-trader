package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Instruction to buy a ticker using a specified cash amount.
 *
 * @param ticker ticker to buy
 * @param cashAmount cash to allocate to the purchase
 * @param executionTime timestamp at which the purchase should execute
 */
public record BuyInstruction(
        String ticker,
        double cashAmount,
        Instant executionTime
) implements TradeInstruction {

    public BuyInstruction {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be blank.");
        }

        if (cashAmount <= 0.0) {
            throw new IllegalArgumentException(
                    "Cash amount must be positive.");
        }

        if (executionTime == null) {
            throw new IllegalArgumentException(
                    "Execution time cannot be null.");
        }
    }
}
