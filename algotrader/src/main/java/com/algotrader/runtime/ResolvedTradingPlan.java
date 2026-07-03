package com.algotrader.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.PredictionType;
import com.algotrader.config.TradeGeneratorType;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable runtime trading plan created by resolving a
 * {@link TradingSessionConfig} into its associated
 * {@link EndpointConfig} and {@link TradeGeneratorConfig}.
 *
 * <p>A {@code ResolvedTradingPlan} represents a fully-resolved trading
 * session ready for runtime execution. It combines the session,
 * endpoint, and trade generation configurations into a single object
 * that can be passed throughout the trading pipeline.
 *
 * <p>This class exists primarily as a convenience layer to avoid
 * repeatedly passing and coordinating multiple configuration objects.
 *
 * <p>The underlying configuration objects remain accessible when
 * lower-level components require direct access to configuration
 * details not exposed through convenience getters.
 */
public final class ResolvedTradingPlan {

    private final TradingSessionConfig sessionConfig;
    private final EndpointConfig endpointConfig;
    private final TradeGeneratorConfig tradeGeneratorConfig;

    public ResolvedTradingPlan(
            TradingSessionConfig sessionConfig,
            EndpointConfig endpointConfig,
            TradeGeneratorConfig tradeGeneratorConfig
    ) {
        validateConstructorArgs(
                sessionConfig,
                endpointConfig,
                tradeGeneratorConfig
        );

        this.sessionConfig = sessionConfig;
        this.endpointConfig = endpointConfig;
        this.tradeGeneratorConfig = tradeGeneratorConfig;
    }

    /**
     * Returns the resolved trading session configuration.
     *
     * @return session configuration
     */
    public TradingSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    /**
     * Returns the resolved endpoint configuration.
     *
     * @return endpoint configuration
     */
    public EndpointConfig getEndpointConfig() {
        return endpointConfig;
    }

    /**
     * Returns the resolved trade generator configuration.
     *
     * @return trade generator configuration
     */
    public TradeGeneratorConfig getTradeGeneratorConfig() {
        return tradeGeneratorConfig;
    }

    /**
     * Convenience method for legacy single-ticker callers.
     *
     * @throws IllegalStateException if this session contains zero or multiple tickers
     */
    public String getTicker() {
        return sessionConfig.getTicker();
    }

    /**
     * Returns all tickers configured for the resolved session.
     *
     * @return configured tickers
     */
    public List<String> getTickers() {
        return sessionConfig.getTickers();
    }

    public TimeInterval getInterval() {
        return endpointConfig.getInterval();
    }

    public String getSessionId() {
        return sessionConfig.getSessionId();
    }

    public String getEndpointId() {
        return sessionConfig.getEndpointId();
    }

    public String getStrategyId() {
        return sessionConfig.getStrategyId();
    }

    public int getNumCandles() {
        return endpointConfig.getNumCandles();
    }

    public Instant getFirstCandleTimestamp() {
        return sessionConfig.getFirstCandleTimestamp();
    }

    public Instant getLastCandleTimestamp() {
        return sessionConfig.getLastCandleTimestamp();
    }

    public Duration getMinTimeBeforeClose() {
        return sessionConfig.getMinTimeBeforeClose();
    }

    public TradeGeneratorType getStrategyType() {
        return tradeGeneratorConfig.getStrategyType();
    }

    public PredictionType getPredictionType() {
        return endpointConfig.getPredictionType();
    }

    public double getStartingCash() {
        return sessionConfig.getStartingCash();
    }

    private static void validateConstructorArgs(
            TradingSessionConfig sessionConfig,
            EndpointConfig endpointConfig,
            TradeGeneratorConfig tradeGeneratorConfig
    ) {
        if (sessionConfig == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }

        if (endpointConfig == null) {
            throw new IllegalArgumentException(
                    "EndpointConfig cannot be null."
            );
        }

        if (tradeGeneratorConfig == null) {
            throw new IllegalArgumentException(
                    "TradeGeneratorConfig cannot be null."
            );
        }
    }
}
