package com.algotrader.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.algotrader.config.TradingPlan;
import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.log.TradingCycleLog;
import com.algotrader.data.log.TradingCycleLog.ActionLog;
import com.algotrader.data.provider.SlidingWindowProvider;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.trader.TradeExecutor;
import com.algotrader.trader.validation.PriceAvailabilityValidator;
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
    private final PriceAvailabilityValidator priceAvailabilityValidator;
    private final TradingCycleLogger tradeLogger;
    private final String ticker;
    private final boolean liveMode = false;

    public TradingCycleRunner(
            TradingPlan tradingPlan,
            SlidingWindowProvider dataProvider,
            TradingStrategy tradingStrategy,
            TradeExecutor tradeExecutor,
            PriceAvailabilityValidator priceAvailabilityValidator,
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
        this.priceAvailabilityValidator = priceAvailabilityValidator;
        this.tradeLogger = tradeLogger;
        this.ticker = tradingPlan.getTicker().toUpperCase();
    }

    /**
     * Initializes the trading cycle runner and loads any required
     * market data before trading cycles begin.
     *
     * @throws DataCacheException if market data cannot be loaded
     */
    public void initialize() throws DataCacheException {
        dataProvider.initialize();
    }

    /**
     * Runs trading cycles until no more historical data is available.
     */
    public void runAllTradingCycles() {
        if (!dataProvider.isInitialized()) {
            throw new IllegalStateException(
                    "TradingCycleRunner must be initialized before "
                            + "running trading cycles."
            );
        }
        int totalCycles = 0;
        System.out.println("Running all trading cycles...");
        while (runTradingCycle()) {
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

        Optional<DataBatch> maybeBatch =
                dataProvider.nextWindow();

        if (maybeBatch.isEmpty()) {
            return false;
        }

        DataBatch batch = maybeBatch.get();

        try {
            List<RoundTripTrade> trades =
                    tradingStrategy.generateTrades(batch);

            List<ActionLog> actionLogs = new ArrayList<>();

            for (RoundTripTrade trade : trades) {

                if (!priceAvailabilityValidator.pricesExist(trade)) {
                    System.out.println(
                            "Skipping trade due to unavailable prices: "
                                    + trade
                    );
                    continue;
                }

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
                            liveMode
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