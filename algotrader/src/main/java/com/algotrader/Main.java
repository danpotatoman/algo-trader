package com.algotrader;

import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradingSessionConfigLoader;
import com.algotrader.logging.JSONTradingCycleLogger;
import com.algotrader.logging.TradingCycleLogger;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.cache.MarketDataCache;
import com.algotrader.marketdata.source.SQLiteOHLCVSource;
import com.algotrader.persistence.OHLCVRepository;
import com.algotrader.persistence.sqlite.repository.SQLiteOHLCVRepository;
import com.algotrader.service.TradingCycleRunner;
import com.algotrader.service.TradingCycleRunnerFactory;

/**
 * Application entry point for running the configured trading session.
 */
public class Main {

    public static void main(String[] args) {

        TradingSessionConfigLoader tradingSessionConfigLoader = new TradingSessionConfigLoader();

        TradingSessionConfig sessionConfig = tradingSessionConfigLoader.load("aapl-cnn-volatility-v1");

        OHLCVRepository repository = new SQLiteOHLCVRepository("data/ohlcv.db");

        MarketDataCache marketDataCache =
            new MarketDataCache(
                        new SQLiteOHLCVSource(repository)
            );

        TradingCycleLogger tradingCycleLogger =
                new JSONTradingCycleLogger();

        TradingCycleRunnerFactory tradingCycleRunnerFactory =
                new TradingCycleRunnerFactory(
                        marketDataCache,
                        marketDataCache,
                        tradingCycleLogger
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
