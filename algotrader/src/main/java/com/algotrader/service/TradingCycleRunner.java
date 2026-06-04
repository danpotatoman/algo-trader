package com.algotrader.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.dataobjects.TradeRecommendation;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.decision.strategy.TradeGenerator;
import com.algotrader.execution.TradeExecutor;
import com.algotrader.execution.validation.PriceAvailabilityValidator;
import com.algotrader.logging.TradingCycleLog;
import com.algotrader.logging.TradingCycleLogger;
import com.algotrader.logging.TradingCycleLog.ActionLog;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.marketdata.provider.SlidingWindowProvider;
import com.algotrader.runtime.ResolvedTradingPlan;

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

    private final SlidingWindowProvider dataProvider;
    private final TradeGenerator tradeGenerator;
    private final TradeExecutor tradeExecutor;
    private final PriceAvailabilityValidator priceAvailabilityValidator;
    private final TradingCycleLogger tradeLogger;
    private final String ticker;
    private final TimeInterval interval;
    private final String sessionId;
    private final String modelId;
    private final String strategyId;
    private final boolean liveMode = false;

    public TradingCycleRunner(
            ResolvedTradingPlan tradingPlan,
            SlidingWindowProvider dataProvider,
            TradeGenerator tradeGenerator,
            TradeExecutor tradeExecutor,
            PriceAvailabilityValidator priceAvailabilityValidator,
            TradingCycleLogger tradeLogger
    ) {
        validateConstructorArgs(
            tradingPlan,
            dataProvider,
            tradeGenerator,
            tradeExecutor,
            priceAvailabilityValidator,
            tradeLogger
        );

        this.dataProvider = dataProvider;
        this.tradeGenerator = tradeGenerator;
        this.tradeExecutor = tradeExecutor;
        this.priceAvailabilityValidator = priceAvailabilityValidator;
        this.tradeLogger = tradeLogger;
        this.ticker = tradingPlan.getTicker().toUpperCase();
        this.interval = tradingPlan.getInterval();
        this.sessionId = tradingPlan.getSessionId();
        this.modelId = tradingPlan.getModelId();
        this.strategyId = tradingPlan.getStrategyId();
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
    public boolean runTradingCycle() {
        long startNanos = System.nanoTime();

        Optional<DataBatch> maybeBatch =
                dataProvider.nextWindow();

        if (maybeBatch.isEmpty()) {
            return false;
        }

        DataBatch batch = maybeBatch.get();

        try {
            List<RoundTripTrade> trades =
                    tradeGenerator.generateTrades(batch);

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
                            ticker,
                            interval,
                            modelId,
                            sessionId,
                            liveMode
                    );

            String cycleId = String.format(
                    "cycle-%s-%s-%s",
                    batch.getFinalTimestamp(),
                    ticker,
                    interval
            );

            TradingCycleLog tradingCycleLog =
                    new TradingCycleLog(
                            cycleId,
                            batch.getFinalTimestamp(),
                            cycleDurationMillis,
                            metadata,
                            strategyId,
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

    private static void validateConstructorArgs(
            ResolvedTradingPlan tradingPlan,
            SlidingWindowProvider dataProvider,
            TradeGenerator tradeGenerator,
            TradeExecutor tradeExecutor,
            PriceAvailabilityValidator priceAvailabilityValidator,
            TradingCycleLogger tradeLogger) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException("ResolvedTradingPlan cannot be null.");
        }

        if (dataProvider == null) {
            throw new IllegalArgumentException("SlidingWindowProvider cannot be null.");
        }

        if (tradeGenerator == null) {
            throw new IllegalArgumentException("TradeGenerator cannot be null.");
        }

        if (tradeExecutor == null) {
            throw new IllegalArgumentException("TradeExecutor cannot be null.");
        }

        if (tradeLogger == null) {
            throw new IllegalArgumentException("TradeLogger cannot be null.");
        }
    }
}