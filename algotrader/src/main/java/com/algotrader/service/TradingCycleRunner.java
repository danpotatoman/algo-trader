package com.algotrader.service;

import java.util.ArrayList;
import java.util.List;

import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.cache.SlidingWindowProvider;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.log.TradeExecutionLog;
import com.algotrader.data.log.TradingCycleLog;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradingCycleLogger;
import com.algotrader.data.trader.TradeExecutor;
import com.algotrader.prediction.PredictionProviderException;
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

    private final SlidingWindowProvider dataProvider;
    private final TradingStrategy tradingStrategy;
    private final TradeExecutor tradeExecutor;
    private final TradingCycleLogger tradeLogger;
    private final String ticker;

    public TradingCycleRunner(
            SlidingWindowProvider dataProvider,
            TradingStrategy tradingStrategy,
            TradeExecutor tradeExecutor,
            TradingCycleLogger tradeLogger,
            String ticker
    ) {
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

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        this.dataProvider = dataProvider;
        this.tradingStrategy = tradingStrategy;
        this.tradeExecutor = tradeExecutor;
        this.tradeLogger = tradeLogger;
        this.ticker = ticker.toUpperCase();
    }

    /**
     * Runs trading cycles until no more historical data is available.
     */
    public void runAllTradingCycles() {
        while (runTradingCycle()) {
            // Continue until the data provider has no more windows.
        }
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
        try {
            DataBatch batch = dataProvider.nextWindow();

            if (!batch.getTicker().equalsIgnoreCase(ticker)) {
                throw new IllegalStateException(
                        "Expected ticker " + ticker
                                + " but received batch for "
                                + batch.getTicker()
                );
            }

            List<TradeRecommendation> recommendations =
                    tradingStrategy.generateRecommendations(batch);

            List<TradeExecutionLog> tradeExecutionLogs =
                    new ArrayList<>();

            for (TradeRecommendation recommendation : recommendations) {
                TradeExecutionLog log =
                        tradeExecutor.handleRecommendation(recommendation);

                tradeExecutionLogs.add(log);
            }

            // TODO: Construct TradingCycleLog object here.
            TradingCycleLog tradingCycleLog = new;

            tradeLogger.log(tradingCycleLog);

            return true;

        } catch (DataCacheException e) {
            System.out.println("No more historical data available for " + ticker);
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