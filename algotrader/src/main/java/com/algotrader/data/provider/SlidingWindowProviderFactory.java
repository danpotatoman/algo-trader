package com.algotrader.data.provider;

import com.algotrader.config.TradingPlan;
import com.algotrader.data.cache.DataCacheException;

/**
 * Factory for constructing {@link SlidingWindowProvider} instances
 * from {@link TradingPlan} configurations.
 */
public class SlidingWindowProviderFactory {

    private final MarketDataProvider marketDataProvider;

    public SlidingWindowProviderFactory(
            MarketDataProvider marketDataProvider
    ) {
        if (marketDataProvider == null) {
            throw new IllegalArgumentException(
                    "MarketDataProvider cannot be null."
            );
        }

        this.marketDataProvider = marketDataProvider;
    }

    /**
     * Creates a SlidingWindowProvider for the supplied trading plan.
     *
     * @param tradingPlan the trading plan configuration
     * @return a configured SlidingWindowProvider
     * @throws DataCacheException if the required market data cannot be loaded
     */
    public SlidingWindowProvider create(
            TradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingPlan cannot be null."
            );
        }

        return new SlidingWindowProvider(
                marketDataProvider,
                tradingPlan.getTicker(),
                tradingPlan.getInterval(),
                tradingPlan.getBatchSize(),
                tradingPlan.getStartingTimestamp(),
                tradingPlan.getEndingTimestamp()
        );
    }
}