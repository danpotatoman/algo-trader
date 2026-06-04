package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Represents a trading decision derived from a model prediction.
 *
 * <p>A {@code TradeRecommendation} encapsulates:
 * <ul>
 *     <li>The ticker to act on</li>
 *     <li>The recommended action (BUY, SELL, HOLD)</li>
 *     <li>The confidence of the decision</li>
 *     <li>The quantity to trade</li>
 *     <li>The timestamp at which the recommendation was made</li>
 * </ul>
 *
 * <p>This class is immutable and is typically produced by a
 * {@code PredictionInterpreter} and consumed by a {@code TradeMaker}.
 */
public final class TradeRecommendation {

    /**
     * Represents the type of trading action to take.
     */
    public enum Action {
        BUY,
        SELL
    }

    private final String ticker;
    private final Action action;
    private final double confidence;
    private final double quantity; //TODO: define what quantity is - number of stocks to buy/sell, or price of stocks to buy/sell?
    private final Instant timestamp;

    /**
     * Constructs a new {@code TradeRecommendation}.
     *
     * @param ticker the ticker symbol (e.g. "AAPL")
     * @param action the trading action (BUY, SELL)
     * @param confidence confidence in the recommendation (0.0 to 1.0)
     * @param quantity the quantity to trade
     * @param timestamp the time the recommendation was generated
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public TradeRecommendation(
            String ticker,
            Action action,
            double confidence,
            double quantity,
            Instant timestamp
    ) {
        validate(ticker, action, confidence, quantity, timestamp);

        this.ticker = ticker;
        this.action = action;
        this.confidence = confidence;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    private void validate(
            String ticker,
            Action action,
            double confidence,
            double quantity,
            Instant timestamp
    ) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null.");
        }

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1.");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }
    }

    public String getTicker() {
        return ticker;
    }

    public Action getAction() {
        return action;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getQuantity() {
        return quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isBuy() {
        return action == Action.BUY;
    }

    public boolean isSell() {
        return action == Action.SELL;
    }

    @Override
    public String toString() {
        return String.format(
                "TradeRecommendation[ticker=%s, action=%s, confidence=%.2f%%, quantity=%.4f, timestamp=%s]",
                ticker,
                action,
                confidence * 100.0,
                quantity,
                timestamp
        );
    }
}