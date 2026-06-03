package com.algotrader.config;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable configuration describing a trading session.
 *
 * <p>A trading session specifies which model and strategy should be used,
 * what ticker should be traded, and the time range over which the session
 * should operate. Model-specific requirements such as interval, batch size,
 * and prediction endpoint are defined by the referenced ModelConfig.
 */
public final class TradingSessionConfig {

    private final String sessionId;

    private final String modelId;
    private final String strategyId;

    private final String ticker;

    private final Duration minTimeBeforeClose;

    private final Instant startingTimestamp;
    private final Instant endingTimestamp;

    public TradingSessionConfig(
            String sessionId,
            String modelId,
            String strategyId,
            String ticker,
            Duration minTimeBeforeClose,
            Instant startingTimestamp,
            Instant endingTimestamp
    ) {
        validateConstructorArgs(
                sessionId,
                modelId,
                strategyId,
                ticker,
                minTimeBeforeClose,
                startingTimestamp,
                endingTimestamp
        );

        this.sessionId = sessionId;
        this.modelId = modelId;
        this.strategyId = strategyId;
        this.ticker = ticker.toUpperCase();

        this.minTimeBeforeClose = minTimeBeforeClose;

        this.startingTimestamp = startingTimestamp;
        this.endingTimestamp = endingTimestamp;
    }

    private static void validateConstructorArgs(
            String sessionId,
            String modelId,
            String strategyId,
            String ticker,
            Duration minTimeBeforeClose,
            Instant startingTimestamp,
            Instant endingTimestamp
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Session ID cannot be null or blank."
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

        if (!startingTimestamp.isBefore(endingTimestamp)) {
            throw new IllegalArgumentException(
                    "Starting timestamp must be before ending timestamp."
            );
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getModelId() {
        return modelId;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public String getTicker() {
        return ticker;
    }

    public Duration getMinTimeBeforeClose() {
        return minTimeBeforeClose;
    }

    public Instant getStartingTimestamp() {
        return startingTimestamp;
    }

    public Instant getEndingTimestamp() {
        return endingTimestamp;
    }

    @Override
    public String toString() {
        return "TradingSessionConfig{" +
                "sessionId='" + sessionId + '\'' +
                ", modelId='" + modelId + '\'' +
                ", strategyId='" + strategyId + '\'' +
                ", ticker='" + ticker + '\'' +
                ", minTimeBeforeClose=" + minTimeBeforeClose +
                ", startingTimestamp=" + startingTimestamp +
                ", endingTimestamp=" + endingTimestamp +
                '}';
    }
}