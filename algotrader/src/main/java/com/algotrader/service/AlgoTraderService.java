package com.algotrader.service;

import java.util.List;
import java.util.concurrent.*;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.buffer.DataBuffer;
import com.algotrader.data.buffer.DataBufferException;
import com.algotrader.data.dataobjects.DataBatch;
import com.algotrader.data.dataobjects.TradeExecutionLog;
import com.algotrader.data.dataobjects.TradeRecommendation;
import com.algotrader.data.log.TradeLogger;
import com.algotrader.data.trader.TradeExecutor;
import com.algotrader.prediction.PredictionProviderException;
import com.algotrader.strategy.TradingStrategy;

/**
 * Orchestrates the algorithmic trading loop by periodically retrieving market data,
 * generating trade recommendations, executing/simulating trades, and logging results.
 */
public class AlgoTraderService {

    private final DataBuffer dataBuffer;
    private final TradingStrategy tradingStrategy;
    private final TradeExecutor tradeExecutor;
    private final TradeLogger tradeLogger;
    private final List<String> tickers;
    private final ScheduledExecutorService scheduler;

    private static final int LOOP_INTERVAL_MINUTES = 5;
    private static final TimeInterval DEFAULT_INTERVAL = TimeInterval.FIVE_MINUTES;
    private static final int DEFAULT_BATCH_SIZE = 10;

    public AlgoTraderService(
            DataBuffer dataBuffer,
            TradingStrategy tradingStrategy,
            TradeExecutor tradeExecutor,
            TradeLogger tradeLogger,
            List<String> tickers
    ) {
        if (dataBuffer == null) {
            throw new IllegalArgumentException("DataBuffer cannot be null.");
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

        if (tickers == null) {
            throw new IllegalArgumentException("Tickers cannot be null.");
        }

        this.dataBuffer = dataBuffer;
        this.tradingStrategy = tradingStrategy;
        this.tradeExecutor = tradeExecutor;
        this.tradeLogger = tradeLogger;
        this.tickers = List.copyOf(tickers);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        runTradingCycle();

        scheduler.scheduleAtFixedRate(
                this::runTradingCycle,
                LOOP_INTERVAL_MINUTES,
                LOOP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public void runTradingCycle() {
        for (String ticker : tickers) {
            try {
                DataBatch batch = dataBuffer.requestBatch(
                        ticker,
                        DEFAULT_INTERVAL,
                        DEFAULT_BATCH_SIZE
                );

                TradeRecommendation[] recommendations =
                        tradingStrategy.generateRecommendations(batch);

                tradeExecutor.handleRecommendations(recommendations);

            } catch (DataBufferException e) {
                System.out.println("No batch available for " + ticker);
                e.printStackTrace();

            } catch (PredictionProviderException e) {
                System.out.println("Prediction failed for " + ticker);
                e.printStackTrace();

            } catch (RuntimeException e) {
                System.out.println("Trading cycle failed for " + ticker);
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}