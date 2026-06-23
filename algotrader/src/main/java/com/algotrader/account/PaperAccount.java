package com.algotrader.account;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal paper trading account implementation.
 *
 * <p>A PaperAccount tracks:
 * <ul>
 *     <li>Account identifier</li>
 *     <li>Cash balance</li>
 *     <li>Current stock positions</li>
 * </ul>
 *
 * <p>The account supports basic buy and sell operations. Buy operations
 * are always accepted and may cause the cash balance to become negative.
 * Sell operations are only accepted when the account owns at least the
 * requested quantity of the specified ticker.
 *
 * <p>This class intentionally does not track:
 * <ul>
 *     <li>Open trades</li>
 *     <li>Trade history</li>
 *     <li>Profit and loss</li>
 *     <li>Cost basis</li>
 *     <li>Margin requirements</li>
 * </ul>
 *
 * <p>Additional accounting functionality can be added in future iterations
 * as the paper trading system evolves.
 */
public final class PaperAccount {

    private final String accountId;
    private double cash;
    private final Map<String, Double> positions;

    /**
     * Creates a new paper trading account.
     *
     * @param accountId unique account identifier
     * @param startingCash initial cash balance
     */
    public PaperAccount(String accountId, double startingCash) {
        this.accountId = accountId;
        this.cash = startingCash;
        this.positions = new HashMap<>();
    }

    /**
     * Purchases shares of a ticker.
     *
     * <p>This operation always succeeds and may result in a negative cash
     * balance.
     *
     * @param ticker stock ticker
     * @param quantity quantity to purchase
     * @param price execution price per share
     * @return always {@code true}
     */
    public synchronized boolean buy(String ticker, double quantity, double price) {
        cash -= quantity * price;
        positions.merge(ticker, quantity, Double::sum);
        return true;
    }

    /**
     * Sells shares of a ticker.
     *
     * <p>The operation succeeds only if the account owns at least the
     * requested quantity.
     *
     * @param ticker stock ticker
     * @param quantity quantity to sell
     * @param price execution price per share
     * @return {@code true} if the sale was executed, {@code false} if the
     *         account did not own enough shares
     */
    public synchronized boolean sell(String ticker, double quantity, double price) {
        double currentQuantity = positions.getOrDefault(ticker, 0.0);

        if (currentQuantity < quantity) {
            return false;
        }

        cash += quantity * price;

        double remainingQuantity = currentQuantity - quantity;

        if (remainingQuantity == 0.0) {
            positions.remove(ticker);
        } else {
            positions.put(ticker, remainingQuantity);
        }

        return true;
    }

    /**
     * Returns the account identifier.
     *
     * @return account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the current cash balance.
     *
     * @return cash balance
     */
    public synchronized double getCash() {
        return cash;
    }

    /**
     * Returns a snapshot of current positions.
     *
     * @return immutable copy of positions by ticker
     */
    public synchronized Map<String, Double> getPositions() {
        return Map.copyOf(positions);
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