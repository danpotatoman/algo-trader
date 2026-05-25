package com.algotrader.data.dataobjects;

import java.util.Arrays;

import com.algotrader.data.trader.TradeExecutor;

/**
 * Represents the result of executing a set of trade recommendations.
 *
 * <p>A {@code TradeExecutionLog} captures:
 * <ul>
 *     <li>The original {@link ModelPrediction} that produced the recommendations</li>
 *     <li>The recommendations that were handled</li>
 *     <li>The resulting net account balance change after execution</li>
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
     * The prediction result that generated the trade recommendations.
     */
    private final ModelPrediction prediction;

    /**
     * The recommendations that were handled/executed.
     */
    private final TradeRecommendation[] recommendations;

    /**
     * Net account balance change resulting from the handled recommendations.
     *
     * <p>Positive values indicate profit, negative values indicate loss.
     */
    private final double netBalanceChange;

    /**
     * Constructs a {@code TradeExecutionLog}.
     *
     * @param prediction the prediction result associated with the execution
     * @param recommendations the handled trade recommendations
     * @param netBalanceChange the resulting net balance change
     *
     * @throws IllegalArgumentException if prediction or recommendations is null
     */
    public TradeExecutionLog(
            ModelPrediction prediction,
            TradeRecommendation[] recommendations,
            double netBalanceChange
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException("ModelPrediction cannot be null.");
        }

        if (recommendations == null) {
            throw new IllegalArgumentException("Recommendations cannot be null.");
        }

        this.prediction = prediction;
        this.recommendations = recommendations.clone();
        this.netBalanceChange = netBalanceChange;
    }

    /**
     * Returns the prediction result associated with this execution.
     *
     * @return the prediction result
     */
    public ModelPrediction getModelPrediction() {
        return prediction;
    }

    /**
     * Returns the handled trade recommendations.
     *
     * @return a copy of the trade recommendations array
     */
    public TradeRecommendation[] getRecommendations() {
        return recommendations.clone();
    }

    /**
     * Returns the net account balance change resulting from execution.
     *
     * @return the net balance change
     */
    public double getNetBalanceChange() {
        return netBalanceChange;
    }

    @Override
    public String toString() {
        return "TradeExecutionLog{" +
                "prediction=" + prediction +
                ", netBalanceChange=" + String.format("%.2f", netBalanceChange) +
                ", recommendations=" + Arrays.toString(recommendations) +
                '}';
    }
}