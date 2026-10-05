package com.algotrader.logging;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.algotrader.decision.dataobjects.*;
import com.algotrader.decision.generator.ClassificationVolatilityPortfolioDecisionGenerator;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.marketdata.model.*;
import com.algotrader.marketdata.provider.MultiTickerWindowProvider;
import com.algotrader.portfolio.*;
import com.algotrader.registry.OpenTradeRegistry;
import com.algotrader.runtime.MultiTickerTradingCycleEvaluator;

class DecisionObservabilityTest {
    private static final Instant TIME = Instant.parse("2026-05-18T14:05:00Z");

    @Test void allSignalsSurviveFilteringAndAllocationRulesAreUnchanged() throws Exception {
        var probabilities = Map.of("AAPL", 0.9, "MSFT", 0.2, "NVDA", 0.5, "AMD", 0.8);
        var batches = probabilities.keySet().stream().sorted().map(DecisionObservabilityTest::batch).toList();
        var generator = generator(probabilities, new AtomicInteger());
        var open = new RoundTripTrade("AMD", 1, TIME.minusSeconds(300), TIME.plusSeconds(1500), "test");
        var result = generator.generateDecision(batches, new PortfolioSnapshot(1000, Map.of("AMD", 1.0)), List.of(open), TIME);
        var signals = new HashMap<String, SignalDecision>();
        result.signals().forEach(s -> signals.put(s.ticker(), s));
        assertEquals(4, signals.size());
        assertEquals("BELOW_PROBABILITY_THRESHOLD", signals.get("MSFT").reason());
        assertEquals("NON_POSITIVE_SCORE", signals.get("NVDA").reason());
        assertEquals("EXTEND_EXISTING_TRADE", signals.get("AMD").reason());
        assertEquals("CAPPED_ALLOCATION", signals.get("AAPL").reason());
        assertEquals(0.9, signals.get("AAPL").probability());
        assertEquals(0.01, signals.get("AAPL").volatility());
        assertEquals(200, signals.get("AAPL").requestedCash());
        assertEquals(200, result.newCapitalAllocations().get(0).cashAmount());
        assertEquals(open.tradeId(), result.openTradeAdjustments().get(0).targetTrade().tradeId());
        assertEquals(4, result.inferenceBatchSize());
        assertTrue(result.predictionDurationNanos() > 0);
    }

    @Test void emptyWindowsSkipInferenceAndNonemptyWorkloadIsExplicit() throws Exception {
        var calls = new AtomicInteger();
        var generator = generator(Map.of("AAPL", 0.9), calls);
        var empty = generator.generateDecision(List.of(), new PortfolioSnapshot(1000, Map.of()), List.of(), TIME);
        assertEquals(0, calls.get());
        assertEquals(0, empty.predictionDurationNanos());
        var portfolio = new PortfolioManager(new PortfolioState(1000));
        var registry = new OpenTradeRegistry(HistoricalRunArtifactTest.policy(TIME.plusSeconds(3600)));
        var windows = new MultiTickerWindowProvider() {
            public void initialize() {}
            public List<DataBatch> windowsAt(Instant time) { return List.of(batch("AAPL")); }
        };
        var evaluation = new MultiTickerTradingCycleEvaluator(windows, generator, portfolio, registry).evaluate(TIME);
        assertEquals(List.of("AAPL"), evaluation.usableTickers());
        assertEquals(1, evaluation.inferenceBatchSize());
        assertEquals("COMPLETED", evaluation.inferenceStatus());
        assertTrue(evaluation.evaluationDurationNanos() >= evaluation.predictionDurationNanos());
    }

    @Test void noCashAndTooLateStillPreservePredictions() throws Exception {
        var generator = generator(Map.of("AAPL", 0.9), new AtomicInteger());
        var noCash = generator.generateDecision(List.of(batch("AAPL")), new PortfolioSnapshot(0, Map.of()), List.of(), TIME);
        assertEquals("NO_DEPLOYABLE_CASH", noCash.signals().get(0).reason());
        var late = generator.generateDecision(List.of(batch("AAPL")), new PortfolioSnapshot(1000, Map.of()), List.of(), TIME.plusSeconds(3300));
        assertEquals("TOO_LATE_TO_ENTER_OR_EXTEND", late.signals().get(0).reason());
        assertEquals(1, late.inferenceBatchSize());
    }

    static DataBatch batch(String ticker) {
        return new DataBatch(TimeInterval.FIVE_MINUTES, new StampedOHLCV(ticker, TimeInterval.FIVE_MINUTES,
                TIME.minusSeconds(300), new OHLCV(100, 101, 99, 100, 1000)));
    }

    private ClassificationVolatilityPortfolioDecisionGenerator generator(Map<String, Double> probabilities, AtomicInteger calls) {
        var provider = new PredictionProvider<List<DataBatch>, List<ClassificationWithVolatilityPrediction>>() {
            public List<ClassificationWithVolatilityPrediction> predict(List<DataBatch> input) {
                calls.incrementAndGet();
                return input.stream().map(b -> new ClassificationWithVolatilityPrediction(b, probabilities.get(b.getTicker()), 0.01, 30)).toList();
            }
            public TimeInterval getInterval() { return TimeInterval.FIVE_MINUTES; }
        };
        return new ClassificationVolatilityPortfolioDecisionGenerator(provider, 0.5, 0.4, 0.2,
                HistoricalRunArtifactTest.policy(TIME.plusSeconds(3600)), "test");
    }
}
