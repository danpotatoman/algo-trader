package com.algotrader.marketdata.provider;

import com.algotrader.runtime.ResolvedTradingPlan;

/**
 * Factory for creating market-data window providers from resolved plans.
 */
public class WindowProviderFactory {
    private final MarketDataProvider marketDataProvider;

    /**
     * Creates a factory backed by the supplied market data provider.
     *
     * @param marketDataProvider provider used by created sliding-window
     *        providers to load market data
     * @throws IllegalArgumentException if {@code marketDataProvider} is null
     */
    public WindowProviderFactory(
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
     * Creates a multi-ticker window provider configured from the supplied
     * trading plan.
     *
     * <p>The returned provider is not initialized. Market data is loaded
     * when {@link MultiTickerWindowProvider#initialize()} is invoked.
     * Preloading includes the completed history needed by the first decision
     * and execution candles through the session end.
     *
     * @param tradingPlan resolved trading plan used to configure the provider
     * @return an uninitialized multi-ticker window provider
     * @throws IllegalArgumentException if {@code tradingPlan} is null
     */
    public MultiTickerWindowProvider createMultiTickerWindowProvider(
        ResolvedTradingPlan tradingPlan
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "ResolvedTradingPlan cannot be null."
            );
        }

        return new DefaultMultiTickerWindowProvider(
            tradingPlan.getTickers(),
            tradingPlan.getInterval(),
            tradingPlan.getNumCandles(),
            this.marketDataProvider,
            tradingPlan.getFirstCandleTimestamp().minus(
                tradingPlan.getInterval().getDuration().multipliedBy(tradingPlan.getNumCandles())),
            tradingPlan.getLastCandleTimestamp()
        );
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
    public SlidingWindowProvider createSlidingWindowProvider(
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
                tradingPlan.getFirstCandleTimestamp(),
                tradingPlan.getLastCandleTimestamp()
        );
    }
}
