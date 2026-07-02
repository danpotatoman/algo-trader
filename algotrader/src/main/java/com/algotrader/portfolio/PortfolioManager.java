package com.algotrader.portfolio;

import java.util.List;
import java.util.Map;

import com.algotrader.execution.TradeExecutionResult;

/**
 * Applies trade execution results to a {@link PortfolioState}.
 *
 * <p>The portfolio manager is responsible for keeping the portfolio state
 * synchronized with successfully executed trades. It does not make trading
 * decisions or execute trades itself.
 *
 * <p>Any inconsistencies between an executed action and the current portfolio
 * state are surfaced by {@link PortfolioState} as exceptions.
 */
public final class PortfolioManager implements PortfolioView {

    private final PortfolioState portfolioState;

    /**
     * Creates a portfolio manager for the supplied portfolio state.
     *
     * @param portfolioState portfolio state to mutate
     * @throws IllegalArgumentException if {@code portfolioState} is null
     */
    public PortfolioManager(PortfolioState portfolioState) {
        if (portfolioState == null) {
            throw new IllegalArgumentException(
                    "Portfolio state cannot be null.");
        }

        this.portfolioState = portfolioState;
    }

    @Override
    public double cash() {
        return portfolioState.cash();
    }

    @Override
    public Map<String, Double> positions() {
        return portfolioState.positions();
    }

    @Override
    public PortfolioSnapshot snapshot() {
        return portfolioState.snapshot();
    }

    /**
     * Applies a trade execution result to the portfolio state.
     *
     * @param action execution result to apply
     * @throws IllegalArgumentException if {@code action} is null
     * @throws IllegalStateException if applying the action would violate
     *         portfolio invariants
     */
    public void apply(TradeExecutionResult action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "Portfolio action cannot be null.");
        }

        switch (action.action()) {
            case BUY -> portfolioState.applyBuy(
                    action.ticker(),
                    action.quantity(),
                    action.price());

            case SELL -> portfolioState.applySell(
                    action.ticker(),
                    action.quantity(),
                    action.price());

            default -> throw new IllegalArgumentException(
                    "Unsupported action: " + action.action());
        }
    }

    /**
     * Applies trade execution results to the portfolio state in order.
     *
     * @param executionResults execution results to apply
     * @throws IllegalArgumentException if {@code actions} is null or contains null
     * @throws IllegalStateException if applying any action would violate portfolio
     *         invariants
     */
    public void applyAll(List<TradeExecutionResult> executionResults) {
        if (executionResults == null) {
            throw new IllegalArgumentException(
                    "Execution results cannot be null.");
        }

        for (TradeExecutionResult executionResult : executionResults) {
            apply(executionResult);
        }
    }
}
