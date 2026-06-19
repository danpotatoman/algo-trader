package com.algotrader.config;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable configuration describing a trading session.
 *
 * <p>A trading session defines one trading workload to be executed by the
 * system. It specifies the ticker to trade, the prediction endpoint to use,
 * the trade generation strategy to use, the active time range for the
 * session, and any market-close trading constraints.
 *
 * <p>This class references endpoint and strategy configurations by ID rather
 * than embedding them directly. At runtime, those IDs are resolved into
 * concrete {@link EndpointConfig} and {@link TradeGeneratorConfig} instances
 * before the session is executed.
 *
 * <p>The {@code firstCandleTimestamp} and {@code lastCandleTimestamp} define the
 * inclusive range of candle timestamps that are valid for the session.
 *
 * <p>The {@code firstCandleTimestamp} is the timestamp of the first candle,
 * chronologically, that is available for model inference. Candles with
 * earlier timestamps are excluded from the session.
 *
 * <p>The {@code lastCandleTimestamp} is the timestamp of the final candle that is
 * available for model inference. Candles with later timestamps are excluded
 * from the session.
 *
 * <p>Endpoint-specific requirements such as candle interval, batch size,
 * feature definitions, and prediction endpoints are defined by the
 * referenced {@link EndpointConfig}.
 */
public final class TradingSessionConfig {

    private final String sessionId;

    private final String endpointId;
    private final String strategyId;

    private final String ticker;

    private final Duration minTimeBeforeClose;

    private final Instant firstCandleTimestamp;
    private final Instant lastCandleTimestamp;

    /**
     * Creates a trading session configuration.
     *
     * @param sessionId unique identifier for the trading session
     * @param endpointId identifier of the endpoint configuration used by the session
     * @param strategyId identifier of the trade generation strategy used by
     *        the session
     * @param ticker ticker symbol to trade
     * @param minTimeBeforeClose minimum time remaining before market close
     *        required to initiate new trades
     * @param firstCandleTimestamp timestamp of the first candle available for
     *        model inference during the session
     * @param lastCandleTimestamp timestamp of the final candle available for
     *        model inference during the session
     * @throws IllegalArgumentException if any argument is invalid
     */
    @JsonCreator
    public TradingSessionConfig(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("endpointId") String endpointId,
            @JsonProperty("strategyId") String strategyId,
            @JsonProperty("ticker") String ticker,
            @JsonProperty("minTimeBeforeClose") Duration minTimeBeforeClose,
            @JsonProperty("firstCandleTimestamp") Instant firstCandleTimestamp,
            @JsonProperty("lastCandleTimestamp") Instant lastCandleTimestamp)
    {
        validateConstructorArgs(
                sessionId,
                endpointId,
                strategyId,
                ticker,
                minTimeBeforeClose,
                firstCandleTimestamp,
                lastCandleTimestamp
        );

        this.sessionId = sessionId;
        this.endpointId = endpointId;
        this.strategyId = strategyId;
        this.ticker = ticker.toUpperCase();

        this.minTimeBeforeClose = minTimeBeforeClose;

        this.firstCandleTimestamp = firstCandleTimestamp;
        this.lastCandleTimestamp = lastCandleTimestamp;
    }

    /**
     * Validates constructor arguments before a trading session configuration
     * is created.
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    private static void validateConstructorArgs(
            String sessionId,
            String endpointId,
            String strategyId,
            String ticker,
            Duration minTimeBeforeClose,
            Instant firstCandleTimestamp,
            Instant lastCandleTimestamp
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Session ID cannot be null or blank."
            );
        }

        if (endpointId == null || endpointId.isBlank()) {
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

        if (firstCandleTimestamp == null) {
            throw new IllegalArgumentException(
                    "Starting timestamp cannot be null."
            );
        }

        if (lastCandleTimestamp == null) {
            throw new IllegalArgumentException(
                    "Ending timestamp cannot be null."
            );
        }

        if (!firstCandleTimestamp.isBefore(lastCandleTimestamp)) {
            throw new IllegalArgumentException(
                    "Starting candle timestamp must be before ending candle timestamp."
            );
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getEndpointId() {
        return endpointId;
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

    public Instant getFirstCandleTimestamp() {
        return firstCandleTimestamp;
    }

    public Instant getLastCandleTimestamp() {
        return lastCandleTimestamp;
    }

    @Override
    public String toString() {
        return "TradingSessionConfig{" +
                "sessionId='" + sessionId + '\'' +
                ", endpointId='" + endpointId + '\'' +
                ", strategyId='" + strategyId + '\'' +
                ", ticker='" + ticker + '\'' +
                ", minTimeBeforeClose=" + minTimeBeforeClose +
                ", firstCandleTimestamp=" + firstCandleTimestamp +
                ", lastCandleTimestamp=" + lastCandleTimestamp +
                '}';
    }
}
