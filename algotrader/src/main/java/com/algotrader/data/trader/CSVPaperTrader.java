package com.algotrader.data.trader;

import com.algotrader.data.buffer.PriceProvider;
import com.algotrader.data.dataobjects.ModelPrediction;
import com.algotrader.data.dataobjects.TradeExecutionLog;
import com.algotrader.data.dataobjects.TradeRecommendation;

/**
 * A {@link TradeExecutor} implementation that simulates trades using a
 * provided historical or real-time price source.
 *
 * <p>This paper trader does not place real trades. Instead, it uses a
 * {@link PriceProvider} to look up ticker prices at each recommendation's
 * timestamp, then calculates the net balance change that would result from
 * executing the recommendations.</p>
 *
 * <p>BUY recommendations decrease balance by {@code price * quantity}.
 * SELL recommendations increase balance by {@code price * quantity}.
 * HOLD recommendations do not affect balance.</p>
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
            throw new IllegalArgumentException("PriceProvider cannot be null.");
        }

        this.priceProvider = priceProvider;
    }

    /**
     * Simulates the net account balance effect of a set of trade recommendations.
     *
     * @param recommendations the recommendations to simulate
     * @throws IllegalArgumentException if recommendations is null
     * @throws RuntimeException if required price data cannot be found
     */
    @Override
    public void handleRecommendations(
            TradeRecommendation[] recommendations
    ) {
        if (recommendations == null) {
            throw new IllegalArgumentException("Recommendations cannot be null.");
        }

        double netBalanceChange = 0.0;

        for (TradeRecommendation recommendation : recommendations) {
            if (recommendation == null) {
                throw new IllegalArgumentException("Recommendation cannot be null.");
            }

            try {
                double executionPrice = priceProvider.getTickerPrice(
                        recommendation.getTicker(),
                        recommendation.getTimestamp()
                );

                double tradeValue = executionPrice * recommendation.getQuantity();

                switch (recommendation.getAction()) {
                    case BUY -> netBalanceChange -= tradeValue;
                    case SELL -> netBalanceChange += tradeValue;
                    case HOLD -> {
                        // No balance change.
                    }
                }

            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Failed to simulate trade recommendation: " + recommendation,
                        e
                );
            }
        }
    }
}