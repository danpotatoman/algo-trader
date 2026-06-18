package com.algotrader.marketdata.provider;

import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for constructing {@link SlidingWindowProvider} instances from
 * {@link ResolvedTradingPlan} definitions.
 *
 * <p>This factory translates trading-plan configuration into a fully
 * configured sliding-window provider capable of producing model input
 * batches for the requested ticker, interval, and time range.
 *
 * <p>The returned provider is not initialized automatically. Callers are
 * responsible for invoking {@link SlidingWindowProvider#initialize()}
 * before requesting windows.
 */
public class SlidingWindowProviderFactory {
    private final MarketDataProvider marketDataProvider;

    /**
     * Creates a factory backed by the supplied market data provider.
     *
     * @param marketDataProvider provider used by created sliding-window
     *        providers to load market data
     * @throws IllegalArgumentException if {@code marketDataProvider} is null
     */
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
     * Creates a sliding-window provider configured from the supplied
     * trading plan.
     *
     * <p>The returned provider is not initialized. Market data is loaded
     * when {@link SlidingWindowProvider#initialize()} is invoked.
     *
     * @param tradingPlan resolved trading plan used to configure the provider
     * @return an uninitialized sliding-window provider
     * @throws IllegalArgumentException if {@code tradingPlan} is null
     */
    public SlidingWindowProvider create(
            ResolvedTradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "ResolvedTradingPlan cannot be null."
            );
        }

        return new SlidingWindowProvider(
                marketDataProvider,
                tradingPlan.getTicker(),
                tradingPlan.getInterval(),
                tradingPlan.getNumCandles(),
                tradingPlan.getStartingTimestamp(),
                tradingPlan.getEndingTimestamp()
        );
    }
}