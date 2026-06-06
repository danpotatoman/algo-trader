package com.algotrader.execution;

import com.algotrader.decision.dataobjects.TradeRecommendation;
import com.algotrader.logging.TradingCycleLog.ActionLog;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;

/**
 * Trade executor that simulates trade execution using market data.
 *
 * <p>This executor does not place real orders. Instead, it looks up the
 * market price associated with a {@link TradeRecommendation} and records
 * the result as an {@link ActionLog}.
 *
 * <p>It is primarily intended for backtesting and simulation workflows,
 * where historical market data is used to estimate trade execution prices.
 *
 * <p>The execution model is intentionally simple: trades are assumed to
 * execute exactly at the market price returned by the configured
 * {@link PriceProvider} for the recommendation timestamp.
 *
 * <p>No slippage, commissions, partial fills, liquidity constraints, or
 * other real-world execution effects are currently modeled.
 */
public class HistoricalPaperTradeExecutor implements TradeExecutor {

    private final PriceProvider priceProvider;

    /**
     * Creates a paper-trading executor.
     *
     * @param priceProvider price source used to determine simulated execution
     *        prices
     * @throws IllegalArgumentException if {@code priceProvider} is null
     */
    public HistoricalPaperTradeExecutor(PriceProvider priceProvider) {
        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        this.priceProvider = priceProvider;
    }

    /**
     * Simulates execution of a trade recommendation.
     *
     * <p>The recommendation is executed using the market price returned by the
     * configured {@link PriceProvider} for the recommendation timestamp.
     *
     * @param recommendation recommendation to execute
     * @return an action log describing the simulated execution
     * @throws IllegalArgumentException if {@code recommendation} is null
     * @throws RuntimeException if execution pricing data cannot be retrieved
     */
    @Override
    public ActionLog handleRecommendation(
            TradeRecommendation recommendation
    ) {
        if (recommendation == null) {
            throw new IllegalArgumentException(
                    "Recommendation cannot be null."
            );
        }
/**
 * TODO: Revisit error handling once the market data layer's missing-data
 * contract is finalized.
 */
        try {
            double executionPrice = priceProvider.getTickerPrice(
                    recommendation.getTicker(),
                    recommendation.getTimestamp()
            ).price();

            return new ActionLog(
                    recommendation.getAction().name(),
                    recommendation.getTimestamp(),
                    executionPrice,
                    recommendation.getQuantity()
            );

        } catch (DataCacheException e) {
            throw new RuntimeException(
                    "Failed to simulate trade recommendation: "
                            + recommendation,
                    e
            );
        }
    }
}