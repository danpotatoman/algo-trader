package com.algotrader.runtime;

import com.algotrader.clock.HistoricalCycleClock;
import com.algotrader.clock.HistoricalCycleClockFactory;
import com.algotrader.execution.HistoricalPaperTradeExecutor;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.logging.MultiTickerTradingSessionLogger;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.portfolio.PortfolioManagerFactory;
import com.algotrader.registry.OpenTradeRegistry;

public final class MultiTickerTradingDriverFactory {

    private final MultiTickerTradingCycleEvaluatorFactory evaluatorFactory;
    private final HistoricalCycleClockFactory historicalClockFactory;
    private final PortfolioManagerFactory portfolioManagerFactory;
    private final PriceProvider priceProvider;

    public MultiTickerTradingDriverFactory(
            MultiTickerTradingCycleEvaluatorFactory evaluatorFactory,
            PriceProvider priceProvider
    ) {
        this.evaluatorFactory = evaluatorFactory;
        this.historicalClockFactory = new HistoricalCycleClockFactory();
        this.portfolioManagerFactory = new PortfolioManagerFactory();
        this.priceProvider = priceProvider;
    }

    public HistoricalMultiTickerBacktestDriver createHistorical(
            ResolvedTradingPlan tradingPlan
    ) {
        OpenTradeRegistry openTradeRegistry =
                new OpenTradeRegistry();

        PortfolioManager portfolioManager =
                portfolioManagerFactory.create(tradingPlan);

        MultiTickerTradingCycleEvaluator evaluator =
                evaluatorFactory.create(tradingPlan, portfolioManager, openTradeRegistry);

        HistoricalCycleClock cycleClock =
                historicalClockFactory.create(tradingPlan);

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
                logger
        );
    }
}