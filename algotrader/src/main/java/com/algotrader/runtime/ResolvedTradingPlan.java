package com.algotrader.runtime;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable runtime trading plan created by resolving a
 * {@link TradingSessionConfig} into its associated
 * {@link ModelConfig} and {@link TradeGeneratorConfig}.
 *
 * <p>A {@code ResolvedTradingPlan} represents a fully-resolved trading
 * session ready for runtime execution. It combines the session,
 * model, and trade generation configurations into a single object
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
    private final ModelConfig modelConfig;
    private final TradeGeneratorConfig tradeGeneratorConfig;

    public ResolvedTradingPlan(
            TradingSessionConfig sessionConfig,
            ModelConfig modelConfig,
            TradeGeneratorConfig tradeGeneratorConfig
    ) {
        validateConstructorArgs(
                sessionConfig,
                modelConfig,
                tradeGeneratorConfig
        );

        this.sessionConfig = sessionConfig;
        this.modelConfig = modelConfig;
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
     * Returns the resolved model configuration.
     *
     * @return model configuration
     */
    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    /**
     * Returns the resolved trade generator configuration.
     *
     * @return trade generator configuration
     */
    public TradeGeneratorConfig getTradeGeneratorConfig() {
        return tradeGeneratorConfig;
    }

    public String getTicker() {
        return sessionConfig.getTicker();
    }

    public TimeInterval getInterval() {
        return modelConfig.getInput().getInterval();
    }

    public String getSessionId() {
        return sessionConfig.getSessionId();
    }

    public String getModelId() {
        return sessionConfig.getModelId();
    }

    public String getStrategyId() {
        return sessionConfig.getStrategyId();
    }

    public int getBatchSize() {
        return modelConfig.getInput().getBatchSize();
    }

    public Instant getStartingTimestamp() {
        return sessionConfig.getStartingTimestamp();
    }

    public Instant getEndingTimestamp() {
        return sessionConfig.getEndingTimestamp();
    }

    public Duration getMinTimeBeforeClose() {
        return sessionConfig.getMinTimeBeforeClose();
    }

    public String getStrategyType() {
        return tradeGeneratorConfig.getStrategyType();
    }

    public String getPredictionType() {
        return modelConfig.getPredictionType();
    }

    /**
     * Returns the confidence threshold configured for the trade generator.
     *
     * <p>This accessor currently assumes the underlying trade generator
     * configuration defines a confidence threshold. Future strategy types
     * may not use confidence-based decision making.
     *
     * @return configured confidence threshold
     *
     * // TODO: Revisit once additional strategy types are supported.
     * // Some strategies may not define a confidence threshold.
     */
    public double getConfidenceThreshold() {
        return tradeGeneratorConfig.getParameters().getConfidenceThreshold(); //TODO: validate that a confidence threshold exists for this model type. Throw if it doesn't. Alternatively, maybe this should be a ClassificationTradingPlan?
    }

    public int getHorizonMinutes() {
        return modelConfig.getOutput().getHorizonMinutes();
    }

    private static void validateConstructorArgs(
            TradingSessionConfig sessionConfig,
            ModelConfig modelConfig,
            TradeGeneratorConfig tradeGeneratorConfig
    ) {
        if (sessionConfig == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }

        if (modelConfig == null) {
            throw new IllegalArgumentException(
                    "ModelConfig cannot be null."
            );
        }

        if (tradeGeneratorConfig == null) {
            throw new IllegalArgumentException(
                    "TradeGeneratorConfig cannot be null."
            );
        }
    }
}