package com.algotrader.service;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.ModelConfigLoader;
import com.algotrader.config.TradingPlan;
import com.algotrader.config.TradingStrategyConfig;
import com.algotrader.config.TradingStrategyConfigLoader;
import com.algotrader.data.cache.SlidingWindowProvider;
import com.algotrader.data.cache.SlidingWindowProviderFactory;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.trader.TradeExecutor;
import com.algotrader.strategy.TradingStrategy;
import com.algotrader.strategy.TradingStrategyFactory;

/**
 * Factory for constructing {@link TradingCycleRunner} instances from
 * {@link TradingPlan} configurations.
 */
public final class TradingCycleRunnerFactory {

    private final ModelConfigLoader modelConfigLoader;
    private final TradingStrategyConfigLoader strategyConfigLoader;
    private final SlidingWindowProviderFactory slidingWindowProviderFactory;
    private final TradingStrategyFactory tradingStrategyFactory;
    private final TradeExecutor tradeExecutor;
    private final TradingCycleLogger tradingCycleLogger;

    public TradingCycleRunnerFactory(
            ModelConfigLoader modelConfigLoader,
            TradingStrategyConfigLoader strategyConfigLoader, //TODO: enforce that prediction type (regression/classification) aligns for modelconfig and strategyconfig
            SlidingWindowProviderFactory slidingWindowProviderFactory,
            TradingStrategyFactory tradingStrategyFactory,
            TradeExecutor tradeExecutor,
            TradingCycleLogger tradingCycleLogger
    ) {
        if (modelConfigLoader == null) {
            throw new IllegalArgumentException(
                    "ModelConfigLoader cannot be null."
            );
        }

        if (strategyConfigLoader == null) {
            throw new IllegalArgumentException(
                    "TradingStrategyConfigLoader cannot be null."
            );
        }

        if (slidingWindowProviderFactory == null) {
            throw new IllegalArgumentException(
                    "SlidingWindowProviderFactory cannot be null."
            );
        }

        if (tradingStrategyFactory == null) {
            throw new IllegalArgumentException(
                    "TradingStrategyFactory cannot be null."
            );
        }

        if (tradeExecutor == null) {
            throw new IllegalArgumentException(
                    "TradeExecutor cannot be null."
            );
        }

        if (tradingCycleLogger == null) {
            throw new IllegalArgumentException(
                    "TradingCycleLogger cannot be null."
            );
        }

        this.modelConfigLoader = modelConfigLoader;
        this.strategyConfigLoader = strategyConfigLoader;
        this.slidingWindowProviderFactory = slidingWindowProviderFactory;
        this.tradingStrategyFactory = tradingStrategyFactory;
        this.tradeExecutor = tradeExecutor;
        this.tradingCycleLogger = tradingCycleLogger;
    }

    /**
     * Creates a {@link TradingCycleRunner} from a trading plan.
     *
     * @param tradingPlan the trading plan configuration
     * @return a configured trading cycle runner
     */
    public TradingCycleRunner create(
            TradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingPlan cannot be null."
            );
        }

        ModelConfig modelConfig =
                modelConfigLoader.load(
                        tradingPlan.getModelId()
                );

        TradingStrategyConfig strategyConfig =
                strategyConfigLoader.load(
                        tradingPlan.getStrategyId()
                );

        SlidingWindowProvider dataProvider =
                slidingWindowProviderFactory.create(
                        tradingPlan
                );

        TradingStrategy tradingStrategy =
                tradingStrategyFactory.create(
                        modelConfig,
                        strategyConfig,
                        tradingPlan
                );

        return new TradingCycleRunner(
                tradingPlan,
                dataProvider,
                tradingStrategy,
                tradeExecutor,
                tradingCycleLogger
        );
    }
}