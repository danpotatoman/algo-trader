package com.algotrader;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.config.TradingPlan;
import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.cache.MarketDataCache;
import com.algotrader.data.log.JSONTradingCycleLogger;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.data.provider.CachedPriceProvider;
import com.algotrader.data.source.SQLiteOHLCVSource;
import com.algotrader.database.OHLCVRepository;
import com.algotrader.database.SQLiteOHLCVRepository;
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
                Duration.ofMinutes(5),
                Instant.parse("2026-05-18T14:20:00Z"),
                Instant.parse("2026-06-01T19:55:00Z")
        );

        OHLCVRepository repository = new SQLiteOHLCVRepository("data/ohlcv.db");

        MarketDataCache marketDataCache =
            new MarketDataCache(
                        new SQLiteOHLCVSource(repository)
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
                        tradeExecutor,
                        tradingCycleLogger,
                        marketDataCache
                );

        TradingCycleRunner tradingCycleRunner =
                tradingCycleRunnerFactory.create(
                        tradingPlan
                );
        
        try {
                tradingCycleRunner.initialize();
        } catch (DataCacheException e) {
                System.out.println("TradingCycleRunner initialization failed.");
        }
        tradingCycleRunner.runAllTradingCycles();
    }
}