package com.algotrader.config;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.config.TradingStrategyConfig.StrategyParameters;
import com.algotrader.data.TimeInterval;

/**
 * Immutable runtime trading plan created by resolving a
 * {@link TradingSessionConfig} into its associated
 * {@link ModelConfig} and {@link TradingStrategyConfig}.
 *
 * <p>This class serves as a convenient aggregation of all
 * configuration required to construct and run a trading session.
 */
public final class ResolvedTradingPlan {

    private final TradingSessionConfig sessionConfig;
    private final ModelConfig modelConfig;
    private final TradingStrategyConfig strategyConfig;

    public ResolvedTradingPlan(
            TradingSessionConfig sessionConfig,
            ModelConfig modelConfig,
            TradingStrategyConfig strategyConfig
    ) {
        validateConstructorArgs(
                sessionConfig,
                modelConfig,
                strategyConfig
        );

        this.sessionConfig = sessionConfig;
        this.modelConfig = modelConfig;
        this.strategyConfig = strategyConfig;
    }

    public TradingSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public TradingStrategyConfig getStrategyConfig() {
        return strategyConfig;
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
        return strategyConfig.getStrategyType();
    }

    public String getPredictionType() {
        return modelConfig.getPredictionType();
    }

    public double getConfidenceThreshold() {
        return strategyConfig.getParameters().getConfidenceThreshold(); //TODO: validate that a confidence threshold exists for this model type. Throw if it doesn't.
    }

    public int getHorizonMinutes() {
        return modelConfig.getOutput().getHorizonMinutes();
    }

    private static void validateConstructorArgs(
            TradingSessionConfig sessionConfig,
            ModelConfig modelConfig,
            TradingStrategyConfig strategyConfig
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

        if (strategyConfig == null) {
            throw new IllegalArgumentException(
                    "TradingStrategyConfig cannot be null."
            );
        }
    }
}