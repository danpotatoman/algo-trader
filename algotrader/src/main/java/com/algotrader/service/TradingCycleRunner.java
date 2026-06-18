package com.algotrader.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.dataobjects.TradeRecommendation;
import com.algotrader.decision.generator.TradeGenerator;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
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
 * Runs historical trading cycles for a resolved trading plan.
 *
 * <p>A {@code TradingCycleRunner} coordinates the main backtesting cycle:
 * it requests the next market-data window, generates proposed trades,
 * validates price availability, executes resulting recommendations, and
 * writes a {@link TradingCycleLog}.
 *
 * <p>This runner is currently designed for historical simulation. It does
 * not schedule future work, run concurrently, or interact with a live broker.
 * Each call to {@link #runTradingCycle()} advances the sliding window by one
 * step and processes any trades generated from that window immediately.
 *
 * <p><b>TODO:</b> Revisit the dependency on
 * {@link PriceAvailabilityValidator}. Price availability may belong in the
 * market data layer or execution layer once the missing-data contract is
 * finalized.
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
    private final String endpointId;
    private final String strategyId;
    private final boolean liveMode = false;

    /**
     * Creates a trading cycle runner.
     *
     * @param tradingPlan resolved trading plan containing session, model, and
     *        strategy metadata
     * @param dataProvider sliding-window provider used to supply model input
     *        batches
     * @param tradeGenerator component that generates round-trip trades from
     *        market data
     * @param tradeExecutor executor used to simulate or perform trade actions
     * @param priceAvailabilityValidator validator used to skip trades whose
     *        required entry or exit prices are unavailable
     * @param tradeLogger logger used to persist completed cycle results
     * @throws IllegalArgumentException if any dependency is null
     */
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
        this.endpointId = tradingPlan.getEndpointId();
        this.strategyId = tradingPlan.getStrategyId();
    }

    /**
     * Initializes the runner by loading the market data required by the sliding
     * window provider.
     *
     * <p>This method must be called before {@link #runTradingCycle()} or
     * {@link #runAllTradingCycles()}.
     *
     * @throws DataCacheException if required market data cannot be loaded
     */
    public void initialize() throws DataCacheException {
        dataProvider.initialize();
    }

    /**
     * Runs trading cycles until no more complete data windows are available.
     *
     * @throws IllegalStateException if the runner has not been initialized
     * @throws RuntimeException if any trading cycle fails
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
     * <p>If a complete data window is available, this method generates trades,
     * skips trades with unavailable execution prices, executes the remaining
     * recommendations, logs the completed cycle, and returns {@code true}.
     *
     * <p>If no complete data window remains, no cycle is run and {@code false}
     * is returned.
     *
     * @return {@code true} if a cycle was run; {@code false} if no data window
     *         remains
     * @throws RuntimeException if prediction generation, trade execution, or
     *         cycle logging fails
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
                            endpointId,
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

        if (priceAvailabilityValidator == null) {
            throw new IllegalArgumentException("PriceAvailabilityValidator cannot be null.");
        }
    }
}