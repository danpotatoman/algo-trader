package com.algotrader;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradingSessionConfigLoader;
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

        TradingSessionConfigLoader tradingSessionConfigLoader = new TradingSessionConfigLoader();

        TradingSessionConfig sessionConfig = tradingSessionConfigLoader.load("aapl-cnn-v1.json");

        OHLCVRepository repository = new SQLiteOHLCVRepository("data/ohlcv.db");

        MarketDataCache marketDataCache =
            new MarketDataCache(
                        new SQLiteOHLCVSource(repository)
            );

        TradeExecutor tradeExecutor =
            new CSVPaperTrader(
                    new CachedPriceProvider(
                            marketDataCache, //TODO: get this in a fectory
                            sessionConfig.getInterval()
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
                        sessionConfig
                );
        
        try {
                tradingCycleRunner.initialize();
        } catch (DataCacheException e) {
                System.out.println("TradingCycleRunner initialization failed.");
        }
        tradingCycleRunner.runAllTradingCycles();
    }
}