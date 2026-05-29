package com.algotrader.service;

import java.util.ArrayList;
import java.util.List;

import com.algotrader.config.TradingPlan;
import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.cache.SlidingWindowProvider;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.log.TradingCycleLog;
import com.algotrader.data.log.TradingCycleLog.ActionLog;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.trader.TradeExecutor;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.strategy.RoundTripTrade;
import com.algotrader.strategy.TradingStrategy;

/**
 * Runs historical trading cycles for a single ticker.
 *
 * <p>This service is intended for backtesting or CSV-based simulation.
 * It does not schedule future cycles or use concurrency. Each call to
 * {@link #runTradingCycle()} advances through the historical data by one
 * window and immediately executes all generated recommendations using
 * historical prices, even if those recommendations are timestamped in
 * the future relative to the current batch.
 */
public class TradingCycleRunner {

    private final TradingPlan tradingPlan;
    private final SlidingWindowProvider dataProvider;
    private final TradingStrategy tradingStrategy;
    private final TradeExecutor tradeExecutor;
    private final TradingCycleLogger tradeLogger;
    private final String ticker;

    public TradingCycleRunner(
            TradingPlan tradingPlan,
            SlidingWindowProvider dataProvider,
            TradingStrategy tradingStrategy,
            TradeExecutor tradeExecutor,
            TradingCycleLogger tradeLogger
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException("TradingPlan cannot be null.");
        }

        if (dataProvider == null) {
            throw new IllegalArgumentException("SlidingWindowProvider cannot be null.");
        }

        if (tradingStrategy == null) {
            throw new IllegalArgumentException("TradingStrategy cannot be null.");
        }

        if (tradeExecutor == null) {
            throw new IllegalArgumentException("TradeExecutor cannot be null.");
        }

        if (tradeLogger == null) {
            throw new IllegalArgumentException("TradeLogger cannot be null.");
        }

        this.tradingPlan = tradingPlan;
        this.dataProvider = dataProvider;
        this.tradingStrategy = tradingStrategy;
        this.tradeExecutor = tradeExecutor;
        this.tradeLogger = tradeLogger;
        this.ticker = dataProvider.getTicker().toUpperCase();
    }

    /**
     * Runs trading cycles until no more historical data is available.
     */
    public void runAllTradingCycles() {
        int totalCycles = 0;
        System.out.println("Running all trading cycles...");
        while (runTradingCycle()) {
            // Continue until the data provider has no more windows.
            totalCycles++;
        }
        System.out.println("Ran for " + totalCycles + " cycles.");
    }

    /**
     * Runs one historical trading cycle.
     *
     * <p>Returns {@code true} if a cycle was successfully run, and
     * {@code false} if no more data was available.
     *
     * @return whether a trading cycle was run
     */
    public boolean runTradingCycle() { // TODO: add a stopping condition?
        long startNanos = System.nanoTime();

        try {
            DataBatch batch = dataProvider.nextWindow();

            if (!batch.getTicker().equalsIgnoreCase(ticker)) {
                throw new IllegalStateException(
                        "Expected ticker " + ticker
                                + " but received batch for "
                                + batch.getTicker()
                );
            }

            List<RoundTripTrade> trades =
                    tradingStrategy.generateTrades(batch);

            List<ActionLog> actionLogs = new ArrayList<>();

            for (RoundTripTrade trade : trades) {
                for (TradeRecommendation recommendation
                        : trade.toRecommendations()) {

                    ActionLog actionLog =
                            tradeExecutor.handleRecommendation(recommendation);

                    actionLogs.add(actionLog);
                }
            }

            long cycleDurationMillis =
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startNanos
                    );

            TradingCycleLog.Metadata metadata =
                    new TradingCycleLog.Metadata(
                            tradingPlan.getTicker(),
                            tradingPlan.getInterval(),
                            tradingPlan.getModelId(),
                            tradingPlan.getPlanId(),
                            tradingPlan.isLiveMode()
                    );

            String cycleId = String.format(
                    "cycle-%s-%s-%s",
                    batch.getFinalTimestamp(),
                    tradingPlan.getTicker(),
                    tradingPlan.getInterval()
            );

            TradingCycleLog tradingCycleLog =
                    new TradingCycleLog(
                            cycleId,
                            batch.getFinalTimestamp(),
                            cycleDurationMillis,
                            metadata,
                            tradingPlan.getStrategyId(),
                            actionLogs
                    );

            tradeLogger.log(tradingCycleLog);

            return true;

        } catch (DataCacheException e) {
            System.out.println("No more historical data available for " + ticker);
            System.out.println(e.getMessage());
            return false;

        } catch (PredictionProviderException e) {
            throw new RuntimeException(
                    "Prediction failed for " + ticker,
                    e
            );

        } catch (RuntimeException e) {
            throw new RuntimeException(
                    "Trading cycle failed for " + ticker,
                    e
            );
        }
    }

    public String getTicker() {
        return ticker;
    }
}