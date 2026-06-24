package com.algotrader.execution;

import com.algotrader.account.PaperAccount;
import com.algotrader.decision.dataobjects.TradeRecommendation;
import com.algotrader.logging.TradingCycleLog.ActionLog;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;

/**
 * Trade executor that simulates trade execution using market data.
 *
 * <p>This executor does not place real orders. Instead, it looks up the
 * market price associated with a {@link TradeRecommendation}, applies the
 * transaction to a {@link PaperAccount}, and records the result as an
 * {@link ActionLog}.
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

    private final PaperAccount paperAccount;

    /**
     * Creates a paper-trading executor.
     *
     * @param priceProvider price source used to determine simulated execution
     *        prices
     * @param paperAccount account updated by simulated executions
     * @throws IllegalArgumentException if either dependency is null
     */
    public HistoricalPaperTradeExecutor(PriceProvider priceProvider, PaperAccount paperAccount) {
        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        if (paperAccount == null) {
            throw new IllegalArgumentException(
                "PaperAccount cannot be null."
            );
        }

        this.priceProvider = priceProvider;
        this.paperAccount = paperAccount;
    }

    /**
     * Simulates execution of a trade recommendation.
     *
     * <p>The recommendation is executed against the configured
     * {@link PaperAccount} using the market price returned by the configured
     * {@link PriceProvider} for the recommendation timestamp.
     *
     * @param recommendation recommendation to execute
     * @return an action log describing the simulated execution
     * @throws IllegalArgumentException if {@code recommendation} is null
     * @throws IllegalStateException if the account rejects the transaction
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

                try {
                        double executionPrice = priceProvider.getTickerPrice(
                                recommendation.getTicker(),
                                recommendation.getExecutionTime()
                        ).price();

                        boolean executed = switch (recommendation.getAction()) {
                                case BUY -> paperAccount.buy(
                                        recommendation.getTicker(),
                                        recommendation.getQuantity(),
                                        executionPrice
                                        );
                                case SELL -> paperAccount.sell(
                                        recommendation.getTicker(),
                                        recommendation.getQuantity(),
                                        executionPrice
                                );
                        };

                        if (!executed) {
                                throw new IllegalStateException(
                                        "Failed to execute trade recommendation: "
                                                + recommendation
                                );
                        }

                        return new ActionLog(
                                recommendation.getAction().name(),
                                recommendation.getExecutionTime(),
                                executionPrice,
                                recommendation.getQuantity()
                        );

                } catch (DataCacheException e) {
                        throw new RuntimeException(
                                "Failed to simulate trade recommendation: "
                                        + recommendation, e
                        );
                }
        }
}
