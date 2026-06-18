package com.algotrader.service;

import com.algotrader.config.EndpointConfig;
import com.algotrader.config.EndpointConfigLoader;
import com.algotrader.config.TradeGeneratorConfig;
import com.algotrader.config.TradingSessionConfig;
import com.algotrader.config.TradeGeneratorConfigLoader;
import com.algotrader.decision.generator.TradeGenerator;
import com.algotrader.decision.generator.TradeGeneratorFactory;
import com.algotrader.decision.prediction.provider.PredictionProviderFactory;
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
 * Factory for constructing {@link TradingCycleRunner} instances from trading
 * session configuration.
 *
 * <p>This factory wires together the components required to run a historical
 * trading cycle, including configuration loading, sliding-window data access,
 * trade generation, simulated execution, price availability checking, and
 * cycle logging.
 *
 * <p>The supplied {@link TradingSessionConfig} references endpoint and
 * strategy configuration by ID. This factory loads those configs, combines
 * them into a {@link ResolvedTradingPlan}, and uses that resolved plan to
 * construct the runtime objects needed by the runner.
 *
 * <p><b>TODO:</b> Revisit the dependency on {@link PriceAvailabilityValidator}
 * once the market data missing-price contract is redesigned.
 */
public final class TradingCycleRunnerFactory {

    private final EndpointConfigLoader modelConfigLoader;
    private final TradeGeneratorConfigLoader tradeGeneratorConfigLoader;
    private final SlidingWindowProviderFactory slidingWindowProviderFactory;
    private final TradeGeneratorFactory tradeGeneratorFactory;
    private final TradeExecutor tradeExecutor;
    private final TradingCycleLogger tradingCycleLogger;
    private final PriceAvailabilityValidator priceAvailabilityValidator;

    /**
     * Creates a trading cycle runner factory.
     *
     * @param marketDataProvider provider used to supply historical OHLCV data
     * @param priceProvider provider used to look up simulated execution prices
     * @param tradingCycleLogger logger used to persist completed cycle logs
     * @throws IllegalArgumentException if any dependency is null
     */
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
                new EndpointConfigLoader();

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
     * Creates a trading cycle runner from a trading session configuration.
     *
     * <p>The session's endpoint and strategy IDs are resolved into concrete
     * configuration objects before the runner is constructed.
     *
     * @param sessionConfig trading session configuration to run
     * @return configured trading cycle runner
     * @throws IllegalArgumentException if {@code sessionConfig} is null
     * @throws RuntimeException if referenced endpoint or strategy configuration
     *         cannot be loaded
     */
    public TradingCycleRunner create(
            TradingSessionConfig sessionConfig
    ) {
        if (sessionConfig == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }

        EndpointConfig modelConfig =
                modelConfigLoader.load(
                        sessionConfig.getEndpointId()
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
