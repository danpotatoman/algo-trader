package com.algotrader.portfolio;

import java.util.Map;

/**
 * Read-only view of a portfolio's current state.
 *
 * <p>This interface exposes portfolio information needed by trading,
 * analytics, and logging components while preventing direct mutation of the
 * underlying portfolio state.
 *
 * <p>Implementations may represent either live or simulated portfolio state.
 */
public interface PortfolioView {

    /**
     * Returns the portfolio's available cash balance.
     *
     * @return available cash
     */
    double cash();

    /**
     * Returns the portfolio's current positions.
     *
     * <p>The returned map associates each ticker symbol with the quantity of
     * shares currently held.
     *
     * @return mapping of ticker symbols to share quantities
     */
    Map<String, Double> positions();

    /**
     * Returns an immutable snapshot of the portfolio's current state.
     *
     * @return portfolio snapshot
     */
    PortfolioSnapshot snapshot();
}
