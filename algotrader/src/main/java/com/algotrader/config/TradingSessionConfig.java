package com.algotrader.config;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable configuration describing a trading session.
 *
 * <p>A trading session defines one trading workload to be executed by the
 * system. It specifies the ticker to trade, the prediction model to use,
 * the trade generation strategy to use, the active time range for the
 * session, and any market-close trading constraints.
 *
 * <p>This class references model and strategy configurations by ID rather
 * than embedding them directly. At runtime, those IDs are resolved into
 * concrete {@link ModelConfig} and {@link TradeGeneratorConfig} instances
 * before the session is executed.
 *
 * <p>The {@code startingTimestamp} and {@code endingTimestamp} define when
 * the session is allowed to operate. These timestamps apply to both
 * backtesting and live trading sessions.
 *
 * <p>Model-specific requirements such as candle interval, batch size,
 * feature definitions, and prediction endpoints are defined by the
 * referenced {@link ModelConfig}.
 */
public final class TradingSessionConfig {

    private final String sessionId;

    private final String modelId;
    private final String strategyId;

    private final String ticker;

    private final Duration minTimeBeforeClose;

    private final Instant startingTimestamp;
    private final Instant endingTimestamp;

    /**
     * Creates a trading session configuration.
     *
     * @param sessionId unique identifier for the trading session
     * @param modelId identifier of the model configuration used by the session
     * @param strategyId identifier of the trade generation strategy used by
     *        the session
     * @param ticker ticker symbol to trade
     * @param minTimeBeforeClose minimum time remaining before market close
     *        required to initiate new trades
     * @param startingTimestamp earliest timestamp at which the session may run
     * @param endingTimestamp timestamp after which the session may no longer run
     * @throws IllegalArgumentException if any argument is invalid
     */
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

    /**
     * Validates constructor arguments before a trading session configuration
     * is created.
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
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