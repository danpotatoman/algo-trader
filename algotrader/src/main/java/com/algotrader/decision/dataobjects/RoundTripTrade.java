package com.algotrader.decision.dataobjects;

import java.time.Instant;
import java.util.List;

/**
 * Represents a complete planned trade consisting of a BUY entry followed by
 * a SELL exit of the same quantity.
 *
 * <p>A {@code RoundTripTrade} describes a fully-defined trading opportunity,
 * including both entry and exit timestamps. By requiring both sides of the
 * trade to be specified together, the trading system can reason about the
 * entire position lifecycle before execution.
 *
 * <p>This object is intended to be the primary output of trade generation
 * components. It serves as an intermediate representation between prediction-
 * based trading decisions and executable {@link TradeInstruction}
 * instances.
 *
 * <p>The entry and exit instructions produced by this object always
 * reference the same ticker and quantity, differing only in action type and
 * execution time. The strategy identifier remains attached to the round trip
 * for attribution.
 */
public record RoundTripTrade(
        String ticker,
        double quantity,
        Instant entryTime,
        Instant exitTime,
        String strategyId
) {

    /**
     * Creates a validated round-trip trade.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public RoundTripTrade {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "ticker cannot be null or blank");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity must be positive");
        }

        if (entryTime == null) {
            throw new IllegalArgumentException(
                    "entryTime cannot be null");
        }

        if (exitTime == null) {
            throw new IllegalArgumentException(
                    "exitTime cannot be null");
        }

        if (!exitTime.isAfter(entryTime)) {
            throw new IllegalArgumentException(
                    "exitTime must be after entryTime");
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "strategyId cannot be null or blank");
        }
    }

    /**
     * Creates the BUY instruction corresponding to the entry leg
     * of this round-trip trade.
     *
     * @return the entry trade instruction
     */
    public TradeInstruction entryInstruction() {
        return new TradeInstruction(
                ticker,
                Action.BUY,
                quantity,
                entryTime
        );
    }

    /**
     * Creates the SELL instruction corresponding to the exit leg
     * of this round-trip trade.
     *
     * @return the exit trade instruction
     */
    public TradeInstruction exitInstruction() {
        return new TradeInstruction(
                ticker,
                Action.SELL,
                quantity,
                exitTime
        );
    }

    /**
     * Returns both entry and exit instructions in execution order.
     *
     * @return a list containing the BUY instruction followed by
     *         the SELL instruction
     */
    public List<TradeInstruction> toInstructions() {
        return List.of(
                entryInstruction(),
                exitInstruction()
        );
    }

    /**
     * Returns a concise human-readable summary of the trade.
     *
     * @return trade summary string
     */
    public String summary() {
        return String.format(
                "%s qty=%d entry=%s exit=%s confidence=%.4f strategy=%s",
                ticker,
                quantity,
                entryTime,
                exitTime,
                strategyId
        );
    }
}
