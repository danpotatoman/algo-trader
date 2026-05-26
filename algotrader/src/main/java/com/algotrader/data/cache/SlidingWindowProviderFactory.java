package com.algotrader.data.cache;

import java.time.Instant;

import com.algotrader.plan.TradingPlan;

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
     * @param startingTime the starting timestamp for traversal
     * @return a configured SlidingWindowProvider
     */
    public SlidingWindowProvider create(
            TradingPlan tradingPlan,
            Instant startingTime
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "TradingPlan cannot be null."
            );
        }

        if (startingTime == null) {
            throw new IllegalArgumentException(
                    "Starting time cannot be null."
            );
        }

        return new SlidingWindowProvider(
                tradingPlan.getTicker(),
                tradingPlan.getInterval(),
                tradingPlan.getBatchSize(),
                startingTime,
                marketDataProvider
        );
    }
}