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
 * {@link ModelConfig} and {@link TradingStrategyConfig}.
 *
 * <p>This class serves as a convenient aggregation of all
 * configuration required to construct and run a trading session.
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

    public TradingSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public TradeGeneratorConfig getStrategyConfig() {
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

    public double getConfidenceThreshold() {
        return tradeGeneratorConfig.getParameters().getConfidenceThreshold(); //TODO: validate that a confidence threshold exists for this model type. Throw if it doesn't.
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
                    "TradingStrategyConfig cannot be null."
            );
        }
    }
}