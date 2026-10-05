package com.algotrader.marketdata.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.algotrader.config.*;
import com.algotrader.decision.dataobjects.*;
import com.algotrader.decision.generator.ClassificationVolatilityPortfolioDecisionGenerator;
import com.algotrader.decision.generator.TradeExitTimePolicy;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.decision.prediction.api.request.ClassificationWithVolatilityRequestMapper;
import com.algotrader.decision.prediction.api.request.BatchClassificationVolatilityRequestMapper;
import com.algotrader.execution.HistoricalPaperTradeExecutor;
import com.algotrader.marketcalendar.UsMarketCalendar;
import com.algotrader.marketdata.model.*;
import com.algotrader.portfolio.PortfolioSnapshot;
import com.algotrader.runtime.ResolvedTradingPlan;

class CompletedCandleTimingTest {
    private static final TimeInterval INTERVAL = TimeInterval.FIVE_MINUTES;
    private static final Instant CYCLE = Instant.parse("2026-05-18T14:05:00Z");

    @Test
    void predictionUsesCompletedCandlesAndExecutionKeepsDecisionTime() throws Exception {
        var data = new InMemoryData(List.of(
                candle(CYCLE.minusSeconds(600), 99),
                candle(CYCLE.minusSeconds(300), 100),
                candle(CYCLE, 200)));
        var windows = new DefaultMultiTickerWindowProvider(List.of("AAPL"), INTERVAL, 2, data);
        var batches = windows.windowsAt(CYCLE);
        assertEquals(1, batches.size());
        assertEquals(List.of(CYCLE.minusSeconds(600), CYCLE.minusSeconds(300)),
                batches.get(0).getRows().stream().map(StampedOHLCV::candleOpenTime).toList());
        assertEquals(CYCLE, batches.get(0).getLastCandleCloseTimestamp());

        var predictions = new PredictionProvider<List<DataBatch>, List<ClassificationWithVolatilityPrediction>>() {
            public List<ClassificationWithVolatilityPrediction> predict(List<DataBatch> input) {
                return input.stream().map(batch ->
                        new ClassificationWithVolatilityPrediction(batch, 0.9, 0.01, 30)).toList();
            }
            public TimeInterval getInterval() { return INTERVAL; }
        };
        var generator = new ClassificationVolatilityPortfolioDecisionGenerator(
                predictions, 0.5, 1.0, 1.0,
                new TradeExitTimePolicy(new UsMarketCalendar(ZoneId.of("America/New_York"),
                        LocalTime.of(9, 30), LocalTime.of(16, 0), Set.of(), Map.of()),
                        INTERVAL, CYCLE.plusSeconds(3600)),
                "test");
        var result = generator.generateDecision(batches, new PortfolioSnapshot(1000, Map.of()), List.of(), CYCLE);
        assertEquals(1, result.newCapitalAllocations().size());
        var allocation = result.newCapitalAllocations().get(0);
        assertEquals(CYCLE, allocation.entryTime());
        assertEquals(Instant.parse("2026-05-18T14:35:00Z"), allocation.plannedExitTime());

        var executor = new HistoricalPaperTradeExecutor((ticker, time) ->
                new MarketPrice(ticker, data.requestRow(ticker, INTERVAL, time).open(), time));
        var execution = executor.handleInstruction(new BuyInstruction(
                allocation.ticker(), allocation.cashAmount(), allocation.entryTime()));
        assertEquals(CYCLE, execution.executionTime());
        assertEquals(200.0, execution.price());
    }

    @Test
    void currentCandleCannotReplaceMissingCompletedHistory() throws Exception {
        var data = new InMemoryData(List.of(candle(CYCLE.minusSeconds(300), 100), candle(CYCLE, 200)));
        var windows = new DefaultMultiTickerWindowProvider(List.of("AAPL"), INTERVAL, 2, data);
        assertTrue(windows.windowsAt(CYCLE).isEmpty());
        var single = new DefaultMultiTickerWindowProvider(List.of("AAPL"), INTERVAL, 1, data);
        assertEquals(CYCLE.minusSeconds(300), single.windowsAt(CYCLE).get(0).getLastCandleTimestamp());
    }

    @Test
    void factoryPreloadsFirstDecisionHistoryAndFinalExecutionCandle() throws Exception {
        var data = new InMemoryData(List.of());
        var end = CYCLE.plusSeconds(3600);
        var session = new TradingSessionConfig("test", "test", "test", List.of("AAPL"), CYCLE, end, 1000);
        var endpoint = new EndpointConfig("test", PredictionType.BATCH_CLASSIFICATION_WITH_VOLATILITY,
                URI.create("http://localhost/predict"), "test", "1", INTERVAL, 30, Map.of());
        var strategy = new TradeGeneratorConfig("test", TradeGeneratorType.THRESHOLD, "test", "1",
                new TradeGeneratorConfig.StrategyParameters(Map.of()));
        new WindowProviderFactory(data).createMultiTickerWindowProvider(
                new ResolvedTradingPlan(session, endpoint, strategy)).initialize();
        assertEquals(CYCLE.minusSeconds(30 * 300), data.preloadStart);
        assertEquals(end, data.preloadEnd);
    }

    private static StampedOHLCV candle(Instant time, double open) {
        return new StampedOHLCV("AAPL", INTERVAL, time, new OHLCV(open, open + 3, open - 1, open + 2, 100));
    }

    @Test
    void configuredRequestsIncludeContextAndThirtyCompletedModelCandles() throws Exception {
        var loader = new EndpointConfigLoader(Path.of("..", "config", "endpoints"));
        var data = new InMemoryData(IntStream.rangeClosed(0, 31)
                .mapToObj(i -> candle(CYCLE.minusSeconds((31L - i) * 300), 100 + i)).toList());
        for (String endpointId : List.of("batch-cnn-v1", "cnn-with-volatility-v1")) {
            var endpoint = loader.load(endpointId);
            assertEquals(31, endpoint.getNumCandles());
            var windows = new DefaultMultiTickerWindowProvider(
                    List.of("AAPL"), endpoint.getInterval(), endpoint.getNumCandles(), data);
            var batches = windows.windowsAt(CYCLE);
            var mapper = new ClassificationWithVolatilityRequestMapper();
            var single = mapper.map(batches.get(0));
            assertEquals(31, single.rows().size());
            assertEquals(CYCLE.minusSeconds(31 * 300).toString(), single.rows().get(0).timestamp());
            assertEquals(CYCLE.minusSeconds(300).toString(), single.rows().get(30).timestamp());
            var batch = new BatchClassificationVolatilityRequestMapper(mapper).map(batches);
            assertEquals(single, batch.batches().get(0));
        }
    }

    private static final class InMemoryData implements MarketDataProvider {
        private final List<StampedOHLCV> rows;
        private Instant preloadStart;
        private Instant preloadEnd;

        private InMemoryData(List<StampedOHLCV> rows) { this.rows = rows; }

        public List<StampedOHLCV> requestRange(String ticker, TimeInterval interval, Instant start, Instant end) {
            return rows.stream().filter(row -> row.ticker().equals(ticker) && row.interval() == interval
                    && !row.candleOpenTime().isBefore(start) && !row.candleOpenTime().isAfter(end)).toList();
        }

        public StampedOHLCV requestRow(String ticker, TimeInterval interval, Instant time) {
            return requestRange(ticker, interval, time, time).get(0);
        }

        public void preloadSession(List<String> tickers, TimeInterval interval, Instant start, Instant end) {
            preloadStart = start;
            preloadEnd = end;
        }
    }
}
