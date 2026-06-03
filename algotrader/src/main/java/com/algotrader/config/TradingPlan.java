package com.algotrader.config;

import java.time.Instant;
import java.time.Duration;

import com.algotrader.data.TimeInterval;

/**
 * Immutable high-level configuration describing a trading setup on historical data.
 */
public final class TradingPlan { //TODO: need to load this from a config

    private final String planId;
    private final String modelId;
    private final String strategyId;

    private final String ticker;
    private final TimeInterval interval;
    private final int batchSize;

    private final Duration minTimeBeforeClose;

    /**
     * Starting timestamp for historical traversal.
     */
    private final Instant startingTimestamp;

    private final Instant endingTimestamp;

    /**
     * Constructs a {@code TradingPlan}.
     *
     * @param planId unique identifier for the plan
     * @param modelId identifier for the prediction model
     * @param strategyId identifier for the interpretation/trading strategy
     * @param ticker ticker symbol to trade
     * @param interval market data interval
     * @param batchSize number of candles required per prediction batch
     * @param startingTimestamp starting timestamp for historical traversal
     */
    public TradingPlan(
            String planId,
            String modelId,
            String strategyId,
            String ticker,
            TimeInterval interval,
            int batchSize,
            Duration minTimeBeforeClose,
            Instant startingTimestamp,
            Instant endingTimestamp
    ) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException(
                    "Plan ID cannot be null or blank."
            );
        }

        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "Model ID cannot be null or blank."
            );
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticker cannot be null or blank."
            );
        }

        if (minTimeBeforeClose == null) {
            throw new IllegalArgumentException(
                    "minTimeBeforeClose cannot be null."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "TimeInterval cannot be null."
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be positive."
            );
        }

        if (startingTimestamp == null) {
            throw new IllegalArgumentException(
                    "Starting timestamp cannot be null."
            );
        }

        if (endingTimestamp == null) {
            throw new IllegalArgumentException(
                    "Ending timestamp cannot be null."
            );
        }

        this.planId = planId;
        this.modelId = modelId;
        this.strategyId = strategyId;

        this.ticker = ticker.toUpperCase();
        this.interval = interval;
        this.batchSize = batchSize;

        this.minTimeBeforeClose = minTimeBeforeClose;
        this.startingTimestamp = startingTimestamp;
        this.endingTimestamp = endingTimestamp;

    }

    /**
     * Returns the unique identifier for this trading plan.
     *
     * @return the plan ID
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * Returns the model identifier associated with this plan.
     *
     * @return the model ID
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Returns the strategy identifier associated with this plan.
     *
     * @return the strategy ID
     */
    public String getStrategyId() {
        return strategyId;
    }

    /**
     * Returns the ticker symbol traded by this plan.
     *
     * @return the ticker symbol
     */
    public String getTicker() {
        return ticker;
    }

    /**
     * Returns the market data interval used by this plan.
     *
     * @return the interval
     */
    public TimeInterval getInterval() {
        return interval;
    }

    /**
     * Returns the number of candles required per prediction batch.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    public Duration getMinTimeBeforeClose() {
        return minTimeBeforeClose;
    }

    /**
     * Returns the starting timestamp for traversal.
     *
     * @return the starting timestamp
     */
    public Instant getStartingTimestamp() {
        return startingTimestamp;
    }

    /**
     * Returns the ending timestamp for traversal.
     *
     * @return the ending timestamp
     */
    public Instant getEndingTimestamp() {
        return endingTimestamp;
    }

    @Override
    public String toString() {
        return "TradingPlan{" +
                "planId='" + planId + '\'' +
                ", modelId='" + modelId + '\'' +
                ", strategyId='" + strategyId + '\'' +
                ", ticker='" + ticker + '\'' +
                ", interval=" + interval +
                ", batchSize=" + batchSize +
                ", startingTimestamp=" + startingTimestamp +
                ", endingTimestamp=" + endingTimestamp +
                '}';
    }
}