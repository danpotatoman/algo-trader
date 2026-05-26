package com.algotrader.plan;

import com.algotrader.data.TimeInterval;

/**
 * Immutable high-level configuration describing a trading setup.
 *
 * <p>A {@code TradingPlan} defines:
 * <ul>
 *     <li>What ticker to trade</li>
 *     <li>What interval to operate on</li>
 *     <li>What model and strategy configuration to use</li>
 *     <li>Whether execution is live or historical</li>
 *     <li>How much historical data each cycle requires</li>
 * </ul>
 *
 * <p>This class is intended to be lightweight and serializable so that
 * trading plans can be loaded from configuration files, persisted,
 * logged, or reconstructed later.
 */
public final class TradingPlan {

    private final String planId;
    private final String modelId;
    private final String strategyId;

    private final String ticker;
    private final TimeInterval interval;

    private final int batchSize;

    /**
     * Whether the trading plan operates on live market data.
     *
     * <p>If false, the plan is assumed to operate on historical/static data.
     */
    private final boolean liveMode;

    /**
     * Constructs a {@code TradingPlan}.
     *
     * @param planId unique identifier for the plan
     * @param modelId identifier for the prediction model
     * @param strategyId identifier for the interpretation/trading strategy
     * @param ticker ticker symbol to trade
     * @param interval market data interval
     * @param batchSize number of candles required per prediction batch
     * @param liveMode whether the plan operates on live data
     */
    public TradingPlan(
            String planId,
            String modelId,
            String strategyId,
            String ticker,
            TimeInterval interval,
            int batchSize,
            boolean liveMode
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

        this.planId = planId;
        this.modelId = modelId;
        this.strategyId = strategyId;

        this.ticker = ticker.toUpperCase();
        this.interval = interval;

        this.batchSize = batchSize;
        this.liveMode = liveMode;
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

    /**
     * Returns whether this plan operates on live market data.
     *
     * @return true if live mode, false if historical/static mode
     */
    public boolean isLiveMode() {
        return liveMode;
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
                ", liveMode=" + liveMode +
                '}';
    }
}