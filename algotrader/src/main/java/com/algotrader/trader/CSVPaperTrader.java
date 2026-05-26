package com.algotrader.trader;

import com.algotrader.data.cache.PriceProvider;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradeExecutionLog;

/**
 * A {@link TradeExecutor} implementation that simulates trades using a
 * provided historical or real-time price source.
 *
 * <p>This paper trader does not place real trades. Instead, it uses a
 * {@link PriceProvider} to look up ticker prices at the recommendation's
 * timestamp and simulates execution.
 *
 * <p>The resulting {@link TradeExecutionLog} stores the executed trade price.
 */
public class CSVPaperTrader implements TradeExecutor {

    private final PriceProvider priceProvider;

    /**
     * Constructs a {@code CSVPaperTrader}.
     *
     * @param priceProvider the price provider used to look up execution prices
     * @throws IllegalArgumentException if {@code priceProvider} is null
     */
    public CSVPaperTrader(PriceProvider priceProvider) {
        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        this.priceProvider = priceProvider;
    }

    /**
     * Simulates execution of a single trade recommendation.
     *
     * @param recommendation the recommendation to simulate
     * @return a TradeExecutionLog describing the simulated execution
     *
     * @throws IllegalArgumentException if recommendation is null
     * @throws RuntimeException if required price data cannot be found
     */
    @Override
    public TradeExecutionLog handleRecommendation(
            TradeRecommendation recommendation
    ) {
        if (recommendation == null) {
            throw new IllegalArgumentException(
                    "Recommendation cannot be null."
            );
        }

        try {
            double executionPrice = priceProvider.getTickerPrice(
                    recommendation.getTicker(),

                    recommendation.getTimestamp()
            ).price();

            return new TradeExecutionLog(
                    recommendation,
                    executionPrice
            );

        } catch (RuntimeException e) {
            throw new RuntimeException(
                    "Failed to simulate trade recommendation: "
                            + recommendation,
                    e
            );
        }
    }
}