package com.algotrader;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.EndpointConfigLoader;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradeGeneratorConfigLoader;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradingSessionConfigLoader;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.marketcalendar.UsMarketCalendar2026Loader;
import com.algotrader.marketdata.cache.MarketDataCache;
import com.algotrader.marketdata.source.SQLiteOHLCVSource;
import com.algotrader.persistence.OHLCVRepository;
import com.algotrader.persistence.sqlite.repository.SQLiteOHLCVRepository;
import com.algotrader.runtime.HistoricalMultiTickerBacktestDriver;
import com.algotrader.runtime.MultiTickerTradingCycleEvaluatorFactory;
import com.algotrader.runtime.MultiTickerTradingDriverFactory;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Application entry point for running the configured trading session.
 */
public class Main {

    public static void main(String[] args) {

        TradingSessionConfigLoader tradingSessionConfigLoader = new TradingSessionConfigLoader();
        EndpointConfigLoader endpointConfigLoader = new EndpointConfigLoader();
        TradeGeneratorConfigLoader tradeGeneratorConfigLoader = new TradeGeneratorConfigLoader();

        TradingSessionConfig sessionConfig = tradingSessionConfigLoader.load("batch-cnn-v1");
        EndpointConfig endpointConfig = endpointConfigLoader.load(sessionConfig.getEndpointId());
        TradeGeneratorConfig tradeGeneratorConfig = tradeGeneratorConfigLoader.load(sessionConfig.getStrategyId());

        ResolvedTradingPlan tradingPlan = new ResolvedTradingPlan(sessionConfig, endpointConfig, tradeGeneratorConfig);

        OHLCVRepository repository = new SQLiteOHLCVRepository("data/ohlcv.db");

        MarketDataCache marketDataCache =
            new MarketDataCache(
                        new SQLiteOHLCVSource(repository)
            );
        
        MarketCalendar calendar = new UsMarketCalendar2026Loader().load();

        MultiTickerTradingCycleEvaluatorFactory evaluatorFactory = new MultiTickerTradingCycleEvaluatorFactory(marketDataCache, marketDataCache, calendar);

        MultiTickerTradingDriverFactory driverFactory = new MultiTickerTradingDriverFactory(evaluatorFactory, marketDataCache, calendar);

        HistoricalMultiTickerBacktestDriver driver = driverFactory.createHistorical(tradingPlan);
        System.out.println("attempting to start cycle driver");
        try {
                driver.run();
        } catch (Exception e) {
                System.out.println("Encountered exception: " + e);
                e.printStackTrace();
        }
    }
}
