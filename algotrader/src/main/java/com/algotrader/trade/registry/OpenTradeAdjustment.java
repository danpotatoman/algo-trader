package com.algotrader.trade.registry;

import java.time.Instant;

import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Represents a requested modification to the planned exit time of an open
 * {@link RoundTripTrade}.
 *
 * <p>An {@code OpenTradeAdjustment} expresses intent from the decision layer.
 * It does not directly modify the open trade registry. Instead, it communicates
 * that the specified trade should have its planned exit time updated.
 *
 * <p>The target trade is identified by its {@link RoundTripTrade} instance.
 * The orchestration layer is responsible for applying the requested adjustment
 * to the {@code OpenTradeRegistry} if appropriate.
 *
 * @param targetTrade the open trade whose planned exit time should be adjusted
 * @param newExitTime the requested new exit time for the trade
 * @param reason human-readable explanation for the adjustment
 */
public record OpenTradeAdjustment(
        RoundTripTrade targetTrade,
        Instant newExitTime,
        String reason
) {

    public OpenTradeAdjustment {
        if (targetTrade == null) {
            throw new IllegalArgumentException("Target trade cannot be null.");
        }

        if (newExitTime == null) {
            throw new IllegalArgumentException("New exit time cannot be null.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank.");
        }
    }
}