package com.algotrader.portfolio;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable representation of portfolio state.
 *
 * <p>A {@code PortfolioState} tracks:
 * <ul>
 *     <li>Cash balance</li>
 *     <li>Current stock positions</li>
 * </ul>
 *
 * <p>This class implements {@link PortfolioView} to expose read-only access
 * to trading, analytics, and logging components. Mutation methods are
 * package-private so portfolio state changes are funneled through authorized
 * classes in this package, such as {@link PortfolioManager}.
 *
 * <p>This class intentionally does not track:
 * <ul>
 *     <li>Open trades</li>
 *     <li>Trade history</li>
 *     <li>Profit and loss</li>
 *     <li>Cost basis</li>
 *     <li>Margin requirements</li>
 * </ul>
 */
public final class PortfolioState implements PortfolioView {

    private double cash;
    private final Map<String, Double> positions;

    /**
     * Creates a new portfolio state.
     *
     * @param startingCash initial cash balance
     */
    public PortfolioState(double startingCash) {
        this.cash = startingCash;
        this.positions = new HashMap<>();
    }

    /**
     * Applies a buy to the portfolio state.
     *
     * <p>The operation succeeds only if the portfolio has enough available
     * cash for the purchase.
     *
     * @param ticker stock ticker
     * @param quantity quantity to purchase
     * @param price execution price per share
     */
    synchronized void applyBuy(
            String ticker,
            double quantity,
            double price) {

        if (quantity <= 0.0) {
            throw new IllegalArgumentException("Buy quantity must be positive.");
        }

        if (price < 0.0) {
            throw new IllegalArgumentException("Buy price must be positive.");
        }

        double purchaseCost = quantity * price;

        if (purchaseCost > cash) {
            throw new IllegalStateException(
                    "Cannot purchase " + quantity + " shares of " + ticker
                    + " for $" + purchaseCost
                    + "; only $" + cash + " is available.");
        }

        cash -= purchaseCost;
        positions.merge(ticker, quantity, Double::sum);
    }

    /**
     * Applies a sell to the portfolio state.
     *
     * <p>The operation succeeds only if the portfolio owns at least the
     * requested quantity.
     *
     * @param ticker stock ticker
     * @param quantity quantity to sell
     * @param price execution price per share
     */
    synchronized void applySell(
            String ticker,
            double quantity,
            double price) {

        if (quantity <= 0.0) {
            throw new IllegalArgumentException("Sell quantity must be positive.");
        }

        if (price < 0.0) {
            throw new IllegalArgumentException("Sell price must be positive.");
        }
        
        double currentQuantity = positions.getOrDefault(ticker, 0.0);

        if (currentQuantity < quantity) {
            throw new IllegalStateException(
            "Cannot sell "
                    + quantity
                    + " shares of "
                    + ticker
                    + "; portfolio only owns "
                    + currentQuantity
                    + '.');
        }

        cash += quantity * price;

        double remainingQuantity = currentQuantity - quantity;

        if (remainingQuantity == 0.0) {
            positions.remove(ticker);
        } else {
            positions.put(ticker, remainingQuantity);
        }
    }

    /**
     * Returns the current cash balance.
     *
     * @return cash balance
     */
    @Override
    public synchronized double cash() {
        return cash;
    }

    /**
     * Returns a snapshot of current positions.
     *
     * @return immutable copy of positions by ticker
     */
    @Override
    public synchronized Map<String, Double> positions() {
        return Map.copyOf(positions);
    }

    /**
     * Returns an immutable snapshot of the current portfolio state.
     *
     * @return portfolio snapshot
     */
    @Override
    public synchronized PortfolioSnapshot snapshot() {
        return new PortfolioSnapshot(cash, positions);
    }

    /**
     * Returns the quantity currently owned for a ticker.
     *
     * @param ticker stock ticker
     * @return owned quantity, or zero if no position exists
     */
    public synchronized double getPosition(String ticker) {
        return positions.getOrDefault(ticker, 0.0);
    }
}
