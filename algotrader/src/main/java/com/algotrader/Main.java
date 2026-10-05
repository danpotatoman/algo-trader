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

    public static void main(String[] args) throws Exception {
        if (args.length > 2 || (args.length == 2 && !"--validate-only".equals(args[1]))
                || (args.length > 0 && args[0].startsWith("--"))) {
            throw new IllegalArgumentException("Usage: java -jar algotrader.jar [session-id [--validate-only]]");
        }
        if (args.length == 2 && "--validate-only".equals(args[1])) {
            var database = java.nio.file.Path.of("data", "ohlcv.db");
            if (!java.nio.file.Files.isRegularFile(database)) {
                throw new IllegalArgumentException("OHLCV database does not exist: " + database);
            }
            var coverage = new com.algotrader.runtime.HistoricalDataCoverageValidator(
                    new SQLiteOHLCVRepository(database.toString()), new UsMarketCalendar2026Loader().load())
                    .validate(loadPlan(args[0]));
            var json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(coverage));
            return;
        }
        var artifacts = new com.algotrader.logging.RunArtifacts(java.nio.file.Path.of("data", "logs", "runs"));
        System.out.println("Preparing run: " + artifacts.directory().toAbsolutePath());
        try {
            run(args, artifacts);
        } catch (Exception e) {
            if (!java.nio.file.Files.exists(artifacts.directory().resolve("completion.json"))) {
                artifacts.failSetup(e);
            }
            throw e;
        }
    }

    private static ResolvedTradingPlan loadPlan(String sessionId) {
        TradingSessionConfigLoader tradingSessionConfigLoader = new TradingSessionConfigLoader();
        EndpointConfigLoader endpointConfigLoader = new EndpointConfigLoader();
        TradeGeneratorConfigLoader tradeGeneratorConfigLoader = new TradeGeneratorConfigLoader();

        TradingSessionConfig sessionConfig = tradingSessionConfigLoader.load(sessionId);
        EndpointConfig endpointConfig = endpointConfigLoader.load(sessionConfig.getEndpointId());
        TradeGeneratorConfig tradeGeneratorConfig = tradeGeneratorConfigLoader.load(sessionConfig.getStrategyId());

        return new ResolvedTradingPlan(sessionConfig, endpointConfig, tradeGeneratorConfig);
    }

    private static void run(String[] args, com.algotrader.logging.RunArtifacts artifacts) throws Exception {
        ResolvedTradingPlan tradingPlan = loadPlan(args.length == 0 ? "batch-cnn-v1" : args[0]);

        artifacts.prepare(java.nio.file.Path.of("."), tradingPlan);
        OHLCVRepository repository = new SQLiteOHLCVRepository(artifacts.database().toString());

        MarketDataCache marketDataCache =
            new MarketDataCache(
                        new SQLiteOHLCVSource(repository)
            );
        
        MarketCalendar calendar = new UsMarketCalendar2026Loader().load();

        var coverage = new com.algotrader.runtime.HistoricalDataCoverageValidator(repository, calendar).validate(tradingPlan);
        artifacts.historicalCoverage(coverage);
        System.out.println("Historical coverage validated: " + coverage.marketCycles() + " market cycles, "
                + coverage.fullUniverseInferenceCycles() + " full-universe inference windows, "
                + coverage.warmupCycles() + " morning warm-up cycles");

        MultiTickerTradingCycleEvaluatorFactory evaluatorFactory = new MultiTickerTradingCycleEvaluatorFactory(marketDataCache, marketDataCache, calendar);

        MultiTickerTradingDriverFactory driverFactory = new MultiTickerTradingDriverFactory(evaluatorFactory, marketDataCache, calendar);

        HistoricalMultiTickerBacktestDriver driver = driverFactory.createHistorical(tradingPlan, artifacts);
        System.out.println("attempting to start cycle driver");
        try {
                var session = driver.run();
                if (session.sessionFailure() != null) {
                    throw new IllegalStateException("Run failed; inspect session.json and completion.json");
                }
        } catch (Exception e) {
                System.out.println("Encountered exception: " + e);
                e.printStackTrace();
                throw e;
        }
    }
}
