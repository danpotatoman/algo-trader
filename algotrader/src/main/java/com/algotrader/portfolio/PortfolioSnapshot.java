package com.algotrader.portfolio;

import java.util.Map;

/**
 * Immutable snapshot of a portfolio's state at a single point in time.
 *
 * <p>A snapshot captures the portfolio's available cash and current
 * positions, allowing trading logs, analytics, and other consumers to
 * retain a historical view of the portfolio even as the underlying
 * portfolio continues to evolve.
 *
 * @param cash available cash balance
 * @param positions mapping of ticker symbols to share quantities
 */
public record PortfolioSnapshot(
        double cash,
        Map<String, Double> positions) {

    /**
     * Creates an immutable portfolio snapshot.
     *
     * <p>The supplied positions map is defensively copied to prevent
     * subsequent modification by callers.
     */
    public PortfolioSnapshot {
        positions = Map.copyOf(positions);
    }
}