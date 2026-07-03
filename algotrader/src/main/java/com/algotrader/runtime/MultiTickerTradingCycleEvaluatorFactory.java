package com.algotrader.runtime;

import com.algotrader.decision.generator.PortfolioDecisionGenerator;
import com.algotrader.decision.generator.TradeGeneratorFactory;
import com.algotrader.marketcalendar.UsMarketCalendar2026Loader;
import com.algotrader.marketdata.provider.MarketDataProvider;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.marketdata.provider.WindowProviderFactory;
import com.algotrader.portfolio.PortfolioView;
import com.algotrader.registry.OpenTradeRegistryView;

/**
 * Factory for constructing multi-ticker trading cycle evaluators from session
 * configuration.
 */
public class MultiTickerTradingCycleEvaluatorFactory {

    private TradeGeneratorFactory tradeGeneratorFactory;
    private WindowProviderFactory windowProviderFactory;
    
    /**
     * Creates a factory backed by the supplied price and market data providers.
     *
     * @param priceProvider provider used by generated trade generators
     * @param marketDataProvider provider used by created window providers
     * @throws IllegalArgumentException if either dependency is null
     */
    public MultiTickerTradingCycleEvaluatorFactory(PriceProvider priceProvider, MarketDataProvider marketDataProvider) {
        if (priceProvider == null) {
            throw new IllegalArgumentException("PriceProvider cannot be null.");
        }

        if (marketDataProvider == null) {
            throw new IllegalArgumentException("MarkdetDataProvider cannot be null.");
        }

        this.tradeGeneratorFactory = new TradeGeneratorFactory(
            new UsMarketCalendar2026Loader().load(),
            priceProvider
        );

        this.windowProviderFactory =
            new WindowProviderFactory(marketDataProvider);
    }

    /**
     * Creates a multi-ticker evaluator for a trading session.
     *
     * @param tradingPlan trading session configuration to resolve
     * @param portfolioView read-only portfolio view used during evaluation
     * @param openTradeRegistryView read-only open trade registry view
     * @return configured multi-ticker trading cycle evaluator
     * @throws IllegalArgumentException if {@code sessionConfig} is null
     */
    public MultiTickerTradingCycleEvaluator create(
            ResolvedTradingPlan tradingPlan,
            PortfolioView portfolioView,
            OpenTradeRegistryView openTradeRegistryView
        ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingSessionConfig cannot be null."
            );
        }    

        PortfolioDecisionGenerator portfolioDecisionGenerator =
            tradeGeneratorFactory.createPortfolioDecisionGenerator(tradingPlan, portfolioView);

        return new MultiTickerTradingCycleEvaluator(
            windowProviderFactory.createMultiTickerWindowProvider(tradingPlan),
            portfolioDecisionGenerator,
            portfolioView,
            openTradeRegistryView
        );
    }
}
