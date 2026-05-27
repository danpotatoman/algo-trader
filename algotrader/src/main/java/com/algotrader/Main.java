package com.algotrader;

import java.time.Instant;
import java.util.List;

import com.algotrader.config.ModelConfigLoader;
import com.algotrader.config.TradingStrategyConfigLoader;
import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.CachedPriceProvider;
import com.algotrader.data.cache.MarketDataCache;
import com.algotrader.data.cache.SlidingWindowProviderFactory;
import com.algotrader.data.log.JSONTradingCycleLogger;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.data.source.CSVOHLCVSource;
import com.algotrader.plan.TradingPlan;
import com.algotrader.prediction.PredictionProviderFactory;
import com.algotrader.strategy.TradingStrategyFactory;
import com.algotrader.service.TradingCycleRunner;
import com.algotrader.service.TradingCycleRunnerFactory;
import com.algotrader.trader.CSVPaperTrader;
import com.algotrader.trader.TradeExecutor;

public class Main {

    public static void main(String[] args) {

        TradingPlan tradingPlan = new TradingPlan(
                "aapl-cnn-v1-5m",
                "cnn-v1",
                "threshold-strategy-v1",
                "AAPL",
                TimeInterval.FIVE_MINUTES,
                10,
                Instant.parse("2026-05-25T10:00:00Z"),
                false
        );

        MarketDataCache marketDataCache =
            new MarketDataCache(
                    List.of(
                            new CSVOHLCVSource("data/ohlcv")
                    )
            );

        SlidingWindowProviderFactory slidingWindowProviderFactory =
                new SlidingWindowProviderFactory(
                        marketDataCache
                );

        ModelConfigLoader modelConfigLoader =
                new ModelConfigLoader();

        TradingStrategyConfigLoader strategyConfigLoader =
                new TradingStrategyConfigLoader();

        PredictionProviderFactory predictionProviderFactory =
                new PredictionProviderFactory();

        TradingStrategyFactory tradingStrategyFactory =
                new TradingStrategyFactory(
                        predictionProviderFactory
                );

        TradeExecutor tradeExecutor =
            new CSVPaperTrader(
                    new CachedPriceProvider(
                            marketDataCache,
                            tradingPlan.getInterval()
                    )
            );

        TradingCycleLogger tradingCycleLogger =
                new JSONTradingCycleLogger();

        TradingCycleRunnerFactory tradingCycleRunnerFactory =
                new TradingCycleRunnerFactory(
                        modelConfigLoader,
                        strategyConfigLoader,
                        slidingWindowProviderFactory,
                        tradingStrategyFactory,
                        tradeExecutor,
                        tradingCycleLogger
                );

        TradingCycleRunner tradingCycleRunner =
                tradingCycleRunnerFactory.create(
                        tradingPlan
                );

        tradingCycleRunner.runAllTradingCycles();
    }
}