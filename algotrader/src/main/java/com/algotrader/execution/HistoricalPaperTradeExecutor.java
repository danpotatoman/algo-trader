package com.algotrader.execution;

import com.algotrader.decision.dataobjects.Action;
import com.algotrader.decision.dataobjects.PortfolioAction;
import com.algotrader.decision.dataobjects.TradeInstruction;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;
import com.algotrader.portfolio.PortfolioManager;
import com.algotrader.portfolio.PortfolioState;

/**
 * Trade executor that simulates trade execution using market data.
 *
 * <p>This executor does not place real orders. Instead, it looks up the
 * market price associated with a {@link TradeInstruction}, applies the
 * transaction to a {@link PortfolioState}, and records the result as an
 * {@link PortfolioAction}.
 *
 * <p>It is primarily intended for backtesting and simulation workflows,
 * where historical market data is used to estimate trade execution prices.
 *
 * <p>The execution model is intentionally simple: trades are assumed to
 * execute exactly at the market price returned by the configured
 * {@link PriceProvider} for the instruction timestamp.
 *
 * <p>No slippage, commissions, partial fills, liquidity constraints, or
 * other real-world execution effects are currently modeled.
 */
public class HistoricalPaperTradeExecutor implements TradeExecutor {

    private final PriceProvider priceProvider;

    private final PortfolioManager portfolioManager;

    /**
     * Creates a paper-trading executor.
     *
     * @param priceProvider price source used to determine simulated execution
     *        prices
     * @param portfolioManager portfolio manager updated by simulated executions
     * @throws IllegalArgumentException if either dependency is null
     */
    public HistoricalPaperTradeExecutor(PriceProvider priceProvider, PortfolioManager portfolioManager) {
        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        if (portfolioManager == null) {
            throw new IllegalArgumentException(
                "PortfolioManager cannot be null."
            );
        }

        this.priceProvider = priceProvider;
        this.portfolioManager = portfolioManager;
    }

    /**
     * Simulates execution of a trade instruction.
     *
     * <p>The instruction is executed against the configured
     * {@link PortfolioState} using the market price returned by the configured
     * {@link PriceProvider} for the instruction timestamp.
     *
     * @param instruction instruction to execute
     * @return a portfolio action describing the simulated execution
     * @throws IllegalArgumentException if {@code instruction} is null
     * @throws IllegalStateException if the account rejects the transaction
     * @throws RuntimeException if execution pricing data cannot be retrieved
     */
    @Override
    public PortfolioAction handleInstruction(
                TradeInstruction instruction
        ) {
                if (instruction == null) {
                        throw new IllegalArgumentException(
                                "Instruction cannot be null."
                        );
                }

                try {
                        double executionPrice = priceProvider.getTickerPrice(
                                instruction.ticker(),
                                instruction.executionTime()
                        ).price();

                        PortfolioAction action = switch (instruction.action()) {
                                case BUY -> new PortfolioAction(instruction.ticker(),
                                        Action.BUY,
                                        instruction.executionTime(),
                                        executionPrice,
                                        instruction.quantity());
                                case SELL -> new PortfolioAction(instruction.ticker(),
                                        Action.SELL,
                                        instruction.executionTime(),
                                        executionPrice,
                                        instruction.quantity());
                        };

                        portfolioManager.apply(action);

                        return action;

                } catch (DataCacheException e) {
                        throw new RuntimeException(
                                "Failed to simulate trade instruction: "
                                        + instruction, e
                        );
                }
        }
}
