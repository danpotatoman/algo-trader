package com.algotrader.service;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.ModelConfigLoader;
import com.algotrader.config.TradingPlan;
import com.algotrader.config.TradingStrategyConfig;
import com.algotrader.config.TradingStrategyConfigLoader;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.data.provider.CachedPriceProvider;
import com.algotrader.data.provider.MarketDataProvider;
import com.algotrader.data.provider.PriceProvider;
import com.algotrader.data.provider.SlidingWindowProvider;
import com.algotrader.data.provider.SlidingWindowProviderFactory;
import com.algotrader.market.MarketCalendar;
import com.algotrader.market.UsMarketCalendar2026Loader;
import com.algotrader.prediction.PredictionProviderFactory;
import com.algotrader.trader.TradeExecutor;
import com.algotrader.trader.validation.PriceAvailabilityValidator;
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
    private final MarketDataProvider marketDataProvider;

    public TradingCycleRunnerFactory(
            TradeExecutor tradeExecutor,
            TradingCycleLogger tradingCycleLogger,
            MarketDataProvider marketDataProvider
    ) {

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

        this.modelConfigLoader =
                new ModelConfigLoader();

        this.strategyConfigLoader =
                new TradingStrategyConfigLoader(); //TODO: enforce that prediction type (regression/classification) aligns for modelconfig and strategyconfig

        this.slidingWindowProviderFactory =
                new SlidingWindowProviderFactory(marketDataProvider);

        PredictionProviderFactory predictionProviderFactory =
                new PredictionProviderFactory();

        UsMarketCalendar2026Loader calendarLoader = new UsMarketCalendar2026Loader();
        MarketCalendar calendar = calendarLoader.load();

        this.tradingStrategyFactory =
                new TradingStrategyFactory(
                        predictionProviderFactory,
                        calendar
                );

        this.marketDataProvider = marketDataProvider;
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

        PriceProvider priceProvider = new CachedPriceProvider(marketDataProvider, tradingPlan.getInterval());

        PriceAvailabilityValidator priceAvailabilityValidator = new PriceAvailabilityValidator(priceProvider); //TODO: maybe its own factory with a create(TradingPlan p) method

        return new TradingCycleRunner(
                tradingPlan,
                dataProvider,
                tradingStrategy,
                tradeExecutor,
                priceAvailabilityValidator,
                tradingCycleLogger
        );
    }
}