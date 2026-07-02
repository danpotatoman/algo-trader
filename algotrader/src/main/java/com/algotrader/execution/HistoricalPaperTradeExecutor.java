package com.algotrader.execution;

import com.algotrader.decision.dataobjects.Action;
import com.algotrader.decision.dataobjects.BuyInstruction;
import com.algotrader.decision.dataobjects.SellInstruction;
import com.algotrader.decision.dataobjects.TradeInstruction;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;

/**
 * Trade executor that simulates trade execution using market data.
 *
 * <p>This executor does not place real orders. Instead, it looks up the
 * market price associated with a {@link TradeInstruction} and returns the
 * simulated result as a {@link TradeExecutionResult}. Portfolio mutation is
 * handled by the orchestration layer after execution succeeds.
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
         * Simulates execution of a trade instruction.
         *
         * <p>The instruction is executed against the configured
         * {@link PriceProvider} for the instruction timestamp.
         *
         * @param instruction instruction to execute
         * @return result describing the simulated execution
         * @throws IllegalArgumentException if {@code instruction} is null
         * @throws TradeExecutionException if execution pricing data cannot be retrieved
         */
        @Override
        public TradeExecutionResult handleInstruction(TradeInstruction instruction) {
                if (instruction == null) {
                        throw new IllegalArgumentException("Instruction cannot be null.");
                }
                try { 
                        double price = priceProvider.getTickerPrice(instruction.ticker(), instruction.executionTime()).price();

                        if (instruction instanceof BuyInstruction buy) {
                                return new TradeExecutionResult(
                                        buy.ticker(),
                                        Action.BUY,
                                        buy.executionTime(),
                                        price,
                                        buy.cashAmount() / price,
                                        buy.cashAmount()
                                );
                        }

                        if (instruction instanceof SellInstruction sell) {
                                return new TradeExecutionResult(
                                        sell.ticker(),
                                        Action.SELL,
                                        sell.executionTime(),
                                        price,
                                        sell.quantity(),
                                        sell.quantity() * price
                                );
                        }

                        throw new TradeExecutionException(
                                "Unsupported trade instruction type: "
                                        + instruction.getClass().getName()
                        );

                } catch(DataCacheException e) {
                        throw new TradeExecutionException(
                                "Unable to get price for " + instruction.ticker() + " at " + instruction.executionTime()
                        );
                }
        }
}
