package com.algotrader.data.log;

import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.trader.TradeExecutor;

/**
 * Represents the result of executing a single trade recommendation.
 *
 * <p>A {@code TradeExecutionLog} captures:
 * <ul>
 *     <li>The executed {@link TradeRecommendation}</li>
 *     <li>The executed trade price</li>
 * </ul>
 *
 * <p>This object is intended to be produced by a {@link TradeExecutor}
 * implementation (such as a CSV paper trader) and consumed by a logger,
 * persistence layer, analytics system, or reporting tool.
 *
 * <p>This class is immutable.
 */
public final class TradeExecutionLog {

    /**
     * The recommendation that was executed.
     */
    private final TradeRecommendation recommendation;

    /**
     * The executed trade price.
     */
    private final double executionPrice;

    /**
     * Constructs a {@code TradeExecutionLog}.
     *
     * @param recommendation the executed trade recommendation
     * @param executionPrice the executed trade price
     *
     * @throws IllegalArgumentException if recommendation is null
     * @throws IllegalArgumentException if executionPrice is negative
     */
    public TradeExecutionLog(
            TradeRecommendation recommendation,
            double executionPrice
    ) {
        if (recommendation == null) {
            throw new IllegalArgumentException(
                    "TradeRecommendation cannot be null."
            );
        }

        if (executionPrice < 0) {
            throw new IllegalArgumentException(
                    "Execution price cannot be negative."
            );
        }

        this.recommendation = recommendation;
        this.executionPrice = executionPrice;
    }

    /**
     * Returns the executed trade recommendation.
     *
     * @return the trade recommendation
     */
    public TradeRecommendation getRecommendation() {
        return recommendation;
    }

    /**
     * Returns the executed trade price.
     *
     * @return the executed trade price
     */
    public double getExecutionPrice() {
        return executionPrice;
    }

    @Override
    public String toString() {
        return "TradeExecutionLog{" +
                "recommendation=" + recommendation +
                ", executionPrice=" +
                String.format("%.2f", executionPrice) +
                '}';
    }
}