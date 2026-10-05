package com.algotrader.runtime;

import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.clock.HistoricalCycleClockFactory;
import com.algotrader.decision.generator.TradeExitTimePolicy;
import com.algotrader.execution.HistoricalPaperTradeExecutor;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.logging.MultiTickerTradingSessionLogger;
import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.portfolio.PortfolioManagerFactory;
import com.algotrader.registry.OpenTradeRegistry;

/**
 * Factory for constructing multi-ticker trading drivers.
 *
 * <p>The factory wires runtime dependencies that are shared by historical
 * sessions, including the market calendar used for trade exit-time policies.
 */
public final class MultiTickerTradingDriverFactory {

    private final MultiTickerTradingCycleEvaluatorFactory evaluatorFactory;
    private final HistoricalCycleClockFactory historicalClockFactory;
    private final PortfolioManagerFactory portfolioManagerFactory;
    private final PriceProvider priceProvider;
    private final MarketCalendar calendar;

    /**
     * Creates a multi-ticker trading driver factory.
     *
     * @param evaluatorFactory factory used to create cycle evaluators
     * @param priceProvider provider used by historical paper execution
     * @param calendar market calendar used for exit-time policy creation
     */
    public MultiTickerTradingDriverFactory(
            MultiTickerTradingCycleEvaluatorFactory evaluatorFactory,
            PriceProvider priceProvider,
            MarketCalendar calendar
    ) {
        this.evaluatorFactory = evaluatorFactory;
        this.historicalClockFactory = new HistoricalCycleClockFactory();
        this.portfolioManagerFactory = new PortfolioManagerFactory();
        this.priceProvider = priceProvider;
        this.calendar = calendar;
    }

    /**
     * Creates a historical backtest driver for the supplied trading plan.
     *
     * @param tradingPlan resolved trading plan to run
     * @return configured historical backtest driver
     */
    public HistoricalMultiTickerBacktestDriver createHistorical(
            ResolvedTradingPlan tradingPlan,
            com.algotrader.logging.RunArtifacts artifacts
    ) {


        TradeExitTimePolicy tradeExitTimePolicy = new TradeExitTimePolicy(calendar, tradingPlan.getInterval(), tradingPlan.getLastCandleTimestamp());

        OpenTradeRegistry openTradeRegistry =
                new OpenTradeRegistry(tradeExitTimePolicy);

        PortfolioManager portfolioManager =
                portfolioManagerFactory.create(tradingPlan);

        MultiTickerTradingCycleEvaluator evaluator =
                evaluatorFactory.create(tradingPlan, portfolioManager, openTradeRegistry);

        HistoricalCycleClock cycleClock =
                historicalClockFactory.create(tradingPlan, calendar);

        TradeExecutor tradeExecutor =
                new HistoricalPaperTradeExecutor(priceProvider);

        MultiTickerTradingSessionLogger logger =
                new MultiTickerTradingSessionLogger();

        return new HistoricalMultiTickerBacktestDriver(
                evaluator,
                cycleClock,
                tradeExecutor,
                portfolioManager,
                openTradeRegistry,
                logger,
                new com.algotrader.portfolio.PortfolioValuator(priceProvider),
                artifacts
        );
    }
}
