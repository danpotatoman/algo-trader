package com.algotrader.service;

import com.algotrader.config.ModelConfig;
import com.algotrader.config.ModelConfigLoader;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradeGeneratorConfigLoader;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
import com.algotrader.decision.strategy.TradeGenerator;
import com.algotrader.decision.strategy.TradeGeneratorFactory;
import com.algotrader.execution.HistoricalPaperTradeExecutor;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.execution.validation.PriceAvailabilityValidator;
import com.algotrader.logging.TradingCycleLogger;
import com.algotrader.marketcalendar.UsMarketCalendar2026Loader;
import com.algotrader.marketdata.provider.MarketDataProvider;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.marketdata.provider.SlidingWindowProvider;
import com.algotrader.marketdata.provider.SlidingWindowProviderFactory;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link TradingCycleRunner} instances from
 * {@link TradingPlan} configurations.
 */
public final class TradingCycleRunnerFactory {

    private final ModelConfigLoader modelConfigLoader;
    private final TradeGeneratorConfigLoader tradeGeneratorConfigLoader;
    private final SlidingWindowProviderFactory slidingWindowProviderFactory;
    private final TradeGeneratorFactory tradeGeneratorFactory;
    private final TradeExecutor tradeExecutor;
    private final TradingCycleLogger tradingCycleLogger;
    private final PriceAvailabilityValidator priceAvailabilityValidator;

    public TradingCycleRunnerFactory(
            MarketDataProvider marketDataProvider,
            PriceProvider priceProvider,
            TradingCycleLogger tradingCycleLogger
    ) {

        if (tradingCycleLogger == null) {
            throw new IllegalArgumentException(
                    "TradingCycleLogger cannot be null."
            );
        }

        if (marketDataProvider == null) {
            throw new IllegalArgumentException(
                    "MarketDataProvider cannot be null."
            );
        }

        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        this.modelConfigLoader =
                new ModelConfigLoader();

        this.tradeGeneratorConfigLoader =
                new TradeGeneratorConfigLoader();

        this.slidingWindowProviderFactory =
                new SlidingWindowProviderFactory(marketDataProvider);      

        this.tradeGeneratorFactory =
                new TradeGeneratorFactory(
                        new PredictionProviderFactory(),
                        new UsMarketCalendar2026Loader().load()
                );
        
        this.tradeExecutor =
                new HistoricalPaperTradeExecutor(
                        priceProvider
                );
        
        this.priceAvailabilityValidator = new PriceAvailabilityValidator(priceProvider);

        this.tradingCycleLogger = tradingCycleLogger;
    }

    /**
     * Creates a {@link TradingCycleRunner} from a trading plan.
     *
     * @param sessionConfig the session configuration
     * @return a configured trading cycle runner
     */
    public TradingCycleRunner create(
            TradingSessionConfig sessionConfig
    ) {
        if (sessionConfig == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }

        ModelConfig modelConfig =
                modelConfigLoader.load(
                        sessionConfig.getModelId()
                );

        TradeGeneratorConfig strategyConfig =
                tradeGeneratorConfigLoader.load(
                        sessionConfig.getStrategyId()
                );
        
        ResolvedTradingPlan tradingPlan = new ResolvedTradingPlan(sessionConfig, modelConfig, strategyConfig);

        SlidingWindowProvider dataProvider =
                slidingWindowProviderFactory.create(
                        tradingPlan
                );

        TradeGenerator tradeGenerator =
                tradeGeneratorFactory.create(
                        tradingPlan
                );

        return new TradingCycleRunner(
                tradingPlan,
                dataProvider,
                tradeGenerator,
                tradeExecutor,
                priceAvailabilityValidator,
                tradingCycleLogger
        );
    }
}