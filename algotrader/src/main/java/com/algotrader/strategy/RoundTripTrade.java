package com.algotrader.strategy;

import java.time.Instant;
import java.util.List;

import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.dataobjects.TradeRecommendation.Action;

/**
 * Represents a complete round-trip trade consisting of a BUY entry
 * followed by a SELL exit of the same quantity.
 *
 * <p>This object is intended to be the primary output of a trading
 * strategy. It guarantees that every trade idea has both an entry
 * and an exit, helping prevent strategies from opening positions
 * that are never intended to be closed.
 */
public record RoundTripTrade(
        String ticker,
        int quantity,
        Instant entryTime,
        Instant exitTime,
        double confidence,
        String strategyId
) {

    /**
     * Validates the round-trip trade parameters.
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

        if (Double.isNaN(confidence)) {
            throw new IllegalArgumentException(
                    "confidence cannot be NaN");
        }
    }

    /**
     * Creates the BUY recommendation corresponding to the entry leg
     * of this round-trip trade.
     *
     * @return the entry trade recommendation
     */
    public TradeRecommendation entryRecommendation() {
        return new TradeRecommendation(
                ticker,
                Action.BUY,
                confidence,
                quantity,
                entryTime
        );
    }

    /**
     * Creates the SELL recommendation corresponding to the exit leg
     * of this round-trip trade.
     *
     * @return the exit trade recommendation
     */
    public TradeRecommendation exitRecommendation() {
        return new TradeRecommendation(
                ticker,
                Action.SELL,
                confidence,
                quantity,
                exitTime
        );
    }

    /**
     * Returns both entry and exit recommendations in execution order.
     *
     * @return a list containing the BUY recommendation followed by
     *         the SELL recommendation
     */
    public List<TradeRecommendation> toRecommendations() {
        return List.of(
                entryRecommendation(),
                exitRecommendation()
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
                confidence,
                strategyId
        );
    }
}