package com.algotrader.logging;

import java.util.List;

import com.algotrader.execution.TradeExecutionResult;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable log object representing one completed trading cycle.
 *
 * <p>A {@code TradingCycleLog} captures the identifying metadata, execution
 * timing, strategy information, and execution results produced during one run
 * of the trading pipeline.
 *
 * <p>This object is intended to be serialized to JSON for persistence,
 * debugging, auditing, backtest analysis, and future analytics workflows.
 */
public final class TradingCycleLog {

    private final String cycleId;
    private final long cycleDurationMillis;

    private final Metadata metadata;

    private final String strategyId;

    private final List<TradeExecutionResult> tradeExecutionResults;

    /**
     * Creates a trading cycle log.
     *
     * @param cycleId unique identifier for the completed cycle
     * @param cycleDurationMillis elapsed runtime of the cycle in milliseconds
     * @param metadata configuration metadata for the cycle
     * @param strategyId strategy used to generate trades
     * @param tradeExecutionResults trade execution results produced by the cycle
     * @throws IllegalArgumentException if any argument is invalid
     */
    public TradingCycleLog(
            String cycleId,
            long cycleDurationMillis,
            Metadata metadata,
            String strategyId,
            List<TradeExecutionResult> tradeExecutionResults
    ) {
        if (cycleId == null || cycleId.isBlank()) {
            throw new IllegalArgumentException(
                    "Cycle ID cannot be null or blank."
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

        if (tradeExecutionResults == null) {
            throw new IllegalArgumentException(
                    "Actions cannot be null."
            );
        }

        this.cycleId = cycleId;
        this.cycleDurationMillis = cycleDurationMillis;

        this.metadata = metadata;

        this.strategyId = strategyId;

        this.tradeExecutionResults = List.copyOf(tradeExecutionResults);
    }

    public String getCycleId() {
        return cycleId;
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

    public List<TradeExecutionResult> getTradeExecutionResults() {
        return tradeExecutionResults;
    }

    /**
     * Immutable metadata describing the configuration used for a trading cycle.
     *
     * <p>This metadata records the endpoint, plan, ticker, interval, and
     * execution mode associated with the cycle so logs can be analyzed without
     * needing to reload the original configuration files.
     */
    public static final class Metadata {

        private final String ticker;
        private final TimeInterval interval;

        private final String endpointId;
        private final String planId;

        private final boolean liveMode;

    /**
     * Creates trading cycle metadata.
     *
     * @param ticker ticker traded during the cycle
     * @param interval market data interval used by the endpoint
     * @param endpointId endpoint used during the cycle
     * @param planId trading plan or session identifier associated with the cycle
     * @param liveMode whether the cycle was run in live mode
     * @throws IllegalArgumentException if any argument is invalid
     */
        public Metadata(
                String ticker,
                TimeInterval interval,
                String endpointId,
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

            if (endpointId == null || endpointId.isBlank()) {
                throw new IllegalArgumentException(
                        "Endpoint ID cannot be null or blank."
                );
            }

            if (planId == null || planId.isBlank()) {
                throw new IllegalArgumentException(
                        "Plan ID cannot be null or blank."
                );
            }

            this.ticker = ticker.toUpperCase();
            this.interval = interval;

            this.endpointId = endpointId;
            this.planId = planId;

            this.liveMode = liveMode;
        }

        public String getTicker() {
            return ticker;
        }

        public TimeInterval getInterval() {
            return interval;
        }

        public String getEndpointId() {
            return endpointId;
        }

        public String getPlanId() {
            return planId;
        }

        public boolean isLiveMode() {
            return liveMode;
        }
    }

}
