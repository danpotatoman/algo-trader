package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable log object representing one completed trading cycle.
 *
 * <p>A {@code TradingCycleLog} captures the identifying metadata, execution
 * timing, strategy information, and executed actions produced during one run
 * of the trading pipeline.
 *
 * <p>This object is intended to be serialized to JSON for persistence,
 * debugging, auditing, backtest analysis, and future analytics workflows.
 */
public final class TradingCycleLog {

    private final String cycleId;
    private final Instant timestamp;
    private final long cycleDurationMillis;

    private final Metadata metadata;

    private final String strategyId;

    private final List<ActionLog> actions;

    /**
     * Creates a trading cycle log.
     *
     * @param cycleId unique identifier for the completed cycle
     * @param timestamp timestamp associated with the cycle
     * @param cycleDurationMillis elapsed runtime of the cycle in milliseconds
     * @param metadata configuration metadata for the cycle
     * @param strategyId strategy used to generate trades
     * @param actions executed trade actions produced by the cycle
     * @throws IllegalArgumentException if any argument is invalid
     */
    public TradingCycleLog(
            String cycleId,
            Instant timestamp,
            long cycleDurationMillis,
            Metadata metadata,
            String strategyId,
            List<ActionLog> actions
    ) {
        if (cycleId == null || cycleId.isBlank()) {
            throw new IllegalArgumentException(
                    "Cycle ID cannot be null or blank."
            );
        }

        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
        }

        if (cycleDurationMillis < 0) {
            throw new IllegalArgumentException(
                    "Cycle duration cannot be negative."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Metadata cannot be null."
            );
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        if (actions == null) {
            throw new IllegalArgumentException(
                    "Actions cannot be null."
            );
        }

        this.cycleId = cycleId;
        this.timestamp = timestamp;
        this.cycleDurationMillis = cycleDurationMillis;

        this.metadata = metadata;

        this.strategyId = strategyId;

        this.actions = List.copyOf(actions);
    }

    public String getCycleId() {
        return cycleId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getCycleDurationMillis() {
        return cycleDurationMillis;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public List<ActionLog> getActions() {
        return actions;
    }

    /**
     * Immutable metadata describing the configuration used for a trading cycle.
     *
     * <p>This metadata records the model, plan, ticker, interval, and execution
     * mode associated with the cycle so logs can be analyzed without needing to
     * reload the original configuration files.
     */
    public static final class Metadata {

        private final String ticker;
        private final TimeInterval interval;

        private final String modelId;
        private final String planId;

        private final boolean liveMode;

    /**
     * Creates trading cycle metadata.
     *
     * @param ticker ticker traded during the cycle
     * @param interval market data interval used by the model
     * @param modelId model used during the cycle
     * @param planId trading plan or session identifier associated with the cycle
     * @param liveMode whether the cycle was run in live mode
     * @throws IllegalArgumentException if any argument is invalid
     */
        public Metadata(
                String ticker,
                TimeInterval interval,
                String modelId,
                String planId,
                boolean liveMode
        ) {
            if (ticker == null || ticker.isBlank()) {
                throw new IllegalArgumentException(
                        "Ticker cannot be null or blank."
                );
            }

            if (interval == null) {
                throw new IllegalArgumentException(
                        "Interval cannot be null."
                );
            }

            if (modelId == null || modelId.isBlank()) {
                throw new IllegalArgumentException(
                        "Model ID cannot be null or blank."
                );
            }

            if (planId == null || planId.isBlank()) {
                throw new IllegalArgumentException(
                        "Plan ID cannot be null or blank."
                );
            }

            this.ticker = ticker.toUpperCase();
            this.interval = interval;

            this.modelId = modelId;
            this.planId = planId;

            this.liveMode = liveMode;
        }

        public String getTicker() {
            return ticker;
        }

        public TimeInterval getInterval() {
            return interval;
        }

        public String getModelId() {
            return modelId;
        }

        public String getPlanId() {
            return planId;
        }

        public boolean isLiveMode() {
            return liveMode;
        }
    }

    /**
     * Immutable log entry describing one executed trade action.
     *
     * <p>An action log records the action type, execution timestamp, execution
     * price, and quantity for a single simulated or real trade action.
     */
    public static final class ActionLog {

        private final String action;

        private final Instant timestamp;

        private final double price;
        private final double quantity;

        /**
         * Creates an executed action log.
         *
         * @param action executed action, such as {@code BUY} or {@code SELL}
         * @param timestamp execution timestamp
         * @param price execution price
         * @param quantity executed quantity
         * @throws IllegalArgumentException if any argument is invalid
         */
        public ActionLog(
                String action,
                Instant timestamp,
                double price,
                double quantity
        ) {
            if (action == null || action.isBlank()) {
                throw new IllegalArgumentException(
                        "Action cannot be null or blank."
                );
            }

            if (timestamp == null) {
                throw new IllegalArgumentException(
                        "Timestamp cannot be null."
                );
            }

            if (price < 0.0) {
                throw new IllegalArgumentException(
                        "Price cannot be negative."
                );
            }

            if (quantity < 0.0) {
                throw new IllegalArgumentException(
                        "Quantity cannot be negative."
                );
            }

            this.action = action;
            this.timestamp = timestamp;
            this.price = price;
            this.quantity = quantity;
        }

        public String getAction() {
            return action;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public double getPrice() {
            return price;
        }

        public double getQuantity() {
            return quantity;
        }
    }
}