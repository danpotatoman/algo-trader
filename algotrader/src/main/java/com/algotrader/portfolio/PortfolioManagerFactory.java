package com.algotrader.portfolio;

import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for creating {@link PortfolioManager} instances for trading sessions.
 *
 * <p>Each created portfolio manager is initialized with a fresh
 * {@link PortfolioState} using the session's configured starting cash.
 */
public final class PortfolioManagerFactory {

    /**
     * Creates a new portfolio manager for the supplied trading session.
     *
     * @param tradingPlan trading session configuration
     * @return initialized portfolio manager
     * @throws IllegalArgumentException if {@code sessionConfig} is {@code null}
     */
    public PortfolioManager create(ResolvedTradingPlan tradingPlan) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "Trading session config cannot be null."
            );
        }

        PortfolioState portfolioState = new PortfolioState(
                tradingPlan.getStartingCash()
        );

        return new PortfolioManager(portfolioState);
    }
}