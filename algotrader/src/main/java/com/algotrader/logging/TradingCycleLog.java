package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable log object representing one completed trading cycle.
 *
 * <p>This class is intended to be serialized to JSON for persistence,
 * analytics, debugging, or replay.
 */
public final class TradingCycleLog {

    private final String cycleId;
    private final Instant timestamp;
    private final long cycleDurationMillis;

    private final Metadata metadata;

    private final String strategyId;

    private final List<ActionLog> actions;

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
     * Immutable metadata describing the trading cycle configuration.
     */
    public static final class Metadata {

        private final String ticker;
        private final TimeInterval interval;

        private final String modelId;
        private final String planId;

        private final boolean liveMode;

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
     * Immutable representation of one executed trade action.
     */
    public static final class ActionLog {

        private final String action;

        private final Instant timestamp;

        private final double price;
        private final double quantity;

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