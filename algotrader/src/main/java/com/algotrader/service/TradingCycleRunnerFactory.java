package com.algotrader.service;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.EndpointConfigLoader;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradeGeneratorConfigLoader;
import com.algotrader.decision.generator.TradeGenerator;
import com.algotrader.decision.generator.TradeGeneratorFactory;
import com.algotrader.execution.HistoricalPaperTradeExecutor;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.execution.validation.PriceAvailabilityValidator;
import com.algotrader.logging.TradingCycleLogger;
import com.algotrader.marketcalendar.UsMarketCalendar2026Loader;
import com.algotrader.marketdata.provider.MarketDataProvider;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.marketdata.provider.SlidingWindowProvider;
import com.algotrader.marketdata.provider.WindowProviderFactory;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.portfolio.PortfolioState;
import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link TradingCycleRunner} instances from trading
 * session configuration.
 *
 * <p>This factory wires together the components required to run a historical
 * trading cycle, including configuration loading, sliding-window data access,
 * trade generation, simulated execution, price availability checking, and
 * cycle logging.
 *
 * <p>The supplied {@link TradingSessionConfig} references endpoint and trade
 * generator configuration by ID. This factory loads those configs, combines
 * them into a {@link ResolvedTradingPlan}, and uses that resolved plan to
 * construct the runtime objects and shared portfolio state needed by the
 * runner.
 */
public final class TradingCycleRunnerFactory {

    private final EndpointConfigLoader endpointConfigLoader;
    private final TradeGeneratorConfigLoader tradeGeneratorConfigLoader;
    private final WindowProviderFactory windowProviderFactory;
    private final TradeGeneratorFactory tradeGeneratorFactory;
    private final PriceProvider priceProvider;

    /**
     * Creates a trading cycle runner factory.
     *
     * @param marketDataProvider provider used to supply historical OHLCV data
     * @param priceProvider provider used to look up simulated execution prices
     * @throws IllegalArgumentException if any dependency is null
     */
    public TradingCycleRunnerFactory(
            MarketDataProvider marketDataProvider,
            PriceProvider priceProvider
    ) {
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

        this.endpointConfigLoader =
                new EndpointConfigLoader();

        this.tradeGeneratorConfigLoader =
                new TradeGeneratorConfigLoader();

        this.windowProviderFactory =
                new WindowProviderFactory(marketDataProvider);      

        this.tradeGeneratorFactory =
                new TradeGeneratorFactory(
                        new UsMarketCalendar2026Loader().load(),
                        priceProvider
                );
        
        this.priceProvider = priceProvider;
    }

    /**
     * Creates a trading cycle runner from a trading session configuration.
     *
     * <p>The session's endpoint and trade generator IDs are resolved into
     * concrete configuration objects before the runner is constructed.
     *
     * @param sessionConfig trading session configuration to run
     * @return configured trading cycle runner
     * @throws IllegalArgumentException if {@code sessionConfig} is null
     * @throws RuntimeException if referenced endpoint or trade generator
     *         configuration cannot be loaded
     */
    public TradingCycleRunner createTradingCycleRunner(
            TradingSessionConfig sessionConfig,
            TradingCycleLogger tradingCycleLogger
    ) {
        if (tradingCycleLogger == null) {
            throw new IllegalArgumentException(
                    "TradingCycleLogger cannot be null."
            );
        }

        if (sessionConfig == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }

        EndpointConfig endpointConfig =
                endpointConfigLoader.load(
                        sessionConfig.getEndpointId()
                );

        TradeGeneratorConfig tradeGeneratorConfig =
                tradeGeneratorConfigLoader.load(
                        sessionConfig.getStrategyId()
                );
        
        ResolvedTradingPlan tradingPlan = new ResolvedTradingPlan(sessionConfig, endpointConfig, tradeGeneratorConfig);

        SlidingWindowProvider dataProvider =
                windowProviderFactory.createSlidingWindowProvider(
                        tradingPlan
                );

        PortfolioManager portfolioManager = new PortfolioManager(new PortfolioState(10000));

        TradeGenerator tradeGenerator =
                tradeGeneratorFactory.create(
                        tradingPlan, portfolioManager
                );
        
        TradeExecutor tradeExecutor =
                new HistoricalPaperTradeExecutor(
                        priceProvider
                );

        return new TradingCycleRunner(
                tradingPlan,
                dataProvider,
                tradeGenerator,
                tradeExecutor,
                new PriceAvailabilityValidator(priceProvider),
                tradingCycleLogger
        );
    }
}
