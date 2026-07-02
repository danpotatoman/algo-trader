package com.algotrader.decision.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.algotrader.decision.dataobjects.CapitalAllocation;
import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.dataobjects.PortfolioDecisionResult;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.prediction.provider.PredictionProvider;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.portfolio.PortfolioSnapshot;
import com.algotrader.trade.registry.OpenTradeAdjustment;

/**
 * Generates multi-ticker portfolio decisions from classification-with-volatility
 * predictions.
 *
 * <p>Signals above the probability threshold may extend existing open trades
 * or allocate cash to new positions, subject to configured allocation limits.
 */
public final class ClassificationVolatilityPortfolioDecisionGenerator
        implements PortfolioDecisionGenerator {

    private static final Duration DEFAULT_HOLDING_PERIOD = Duration.ofMinutes(30);

    private final PredictionProvider<
            List<DataBatch>,
            List<ClassificationWithVolatilityPrediction>
            > predictionProvider;

    private final double minProbabilityThreshold;
    private final double cashAllocationFraction;
    private final double maxAllocationFractionPerTicker;
    private final String strategyId;

    /**
     * Creates a classification-with-volatility portfolio decision generator.
     *
     * @param predictionProvider provider used to fetch predictions for batches
     * @param minProbabilityThreshold minimum upward probability required
     * @param cashAllocationFraction fraction of available cash to deploy
     * @param maxAllocationFractionPerTicker maximum cash fraction per ticker
     * @param strategyId strategy identifier assigned to generated allocations
     * @throws IllegalArgumentException if any dependency or threshold is invalid
     */
    public ClassificationVolatilityPortfolioDecisionGenerator(
            PredictionProvider<
                    List<DataBatch>,
                    List<ClassificationWithVolatilityPrediction>
                    > predictionProvider,
            double minProbabilityThreshold,
            double cashAllocationFraction,
            double maxAllocationFractionPerTicker,
            String strategyId
    ) {
        if (predictionProvider == null) {
            throw new IllegalArgumentException("predictionProvider cannot be null.");
        }
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("strategyId cannot be blank.");
        }
        if (minProbabilityThreshold < 0.0 || minProbabilityThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "minProbabilityThreshold must be in [0, 1]."
            );
        }
        if (cashAllocationFraction < 0.0 || cashAllocationFraction > 1.0) {
            throw new IllegalArgumentException(
                    "cashAllocationFraction must be in [0, 1]."
            );
        }
        if (maxAllocationFractionPerTicker < 0.0 || maxAllocationFractionPerTicker > 1.0) {
            throw new IllegalArgumentException(
                    "maxAllocationFractionPerTicker must be in [0, 1]."
            );
        }

        this.predictionProvider = predictionProvider;
        this.minProbabilityThreshold = minProbabilityThreshold;
        this.cashAllocationFraction = cashAllocationFraction;
        this.maxAllocationFractionPerTicker = maxAllocationFractionPerTicker;
        this.strategyId = strategyId;
    }

    /**
     * Generates allocation and open-trade adjustment decisions for one cycle.
     *
     * @param batches market data batches for the current cycle
     * @param portfolio portfolio snapshot provider for allocation sizing
     * @param openTrades trades already open at the start of the cycle
     * @param cycleTime trading cycle timestamp
     * @return portfolio decision result for the cycle
     * @throws DecisionGenerationException if prediction generation fails
     */
    @Override
    public PortfolioDecisionResult generateDecision(
            List<DataBatch> batches,
            PortfolioSnapshot portfolio,
            List<RoundTripTrade> openTrades,
            Instant cycleTime
    ) throws DecisionGenerationException {

        List<ClassificationWithVolatilityPrediction> predictions;

        try {
                predictions =
                        predictionProvider.predict(batches);
        } catch (PredictionProviderException e) {
                throw new DecisionGenerationException(
                        "Failed to generate portfolio decision.",
                        e);
        }

        List<ClassificationWithVolatilityPrediction> acceptedPredictions =
                predictions.stream()
                        .filter(this::passesSignalFilter)
                        .toList();

        Map<String, List<RoundTripTrade>> openTradesByTicker =
                openTrades.stream()
                        .collect(Collectors.groupingBy(RoundTripTrade::ticker));

        List<OpenTradeAdjustment> openTradeAdjustments =
                buildOpenTradeAdjustments(
                        acceptedPredictions,
                        openTradesByTicker,
                        cycleTime
                );

        List<ClassificationWithVolatilityPrediction> newAllocationCandidates =
                acceptedPredictions.stream()
                        .filter(prediction ->
                                !openTradesByTicker.containsKey(prediction.getTicker()))
                        .toList();

        List<CapitalAllocation> capitalAllocations = //TODO: reconsider allocation logic eventually.
                buildCapitalAllocations(
                        newAllocationCandidates,
                        portfolio,
                        cycleTime
                );

        return new PortfolioDecisionResult(
                capitalAllocations,
                openTradeAdjustments
        );
    }

    private boolean passesSignalFilter(
            ClassificationWithVolatilityPrediction prediction
    ) {
        return prediction.probability() >= minProbabilityThreshold;
    }

    private List<OpenTradeAdjustment> buildOpenTradeAdjustments(
            List<ClassificationWithVolatilityPrediction> acceptedPredictions,
            Map<String, List<RoundTripTrade>> openTradesByTicker,
            Instant cycleTime
    ) {
        Instant newExitTime = cycleTime.plus(DEFAULT_HOLDING_PERIOD);
        List<OpenTradeAdjustment> adjustments = new ArrayList<>();

        for (ClassificationWithVolatilityPrediction prediction : acceptedPredictions) {
            List<RoundTripTrade> matchingOpenTrades =
                    openTradesByTicker.getOrDefault(
                            prediction.getTicker(),
                            List.of()
                    );

            for (RoundTripTrade openTrade : matchingOpenTrades) {
                adjustments.add(new OpenTradeAdjustment(
                        openTrade,
                        newExitTime,
                        "Signal remained above probability threshold; extending exit time."
                ));
            }
        }

        return adjustments;
    }

    private List<CapitalAllocation> buildCapitalAllocations(
            List<ClassificationWithVolatilityPrediction> candidates,
            PortfolioSnapshot portfolio,
            Instant cycleTime
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        double cashToDeploy = portfolio.cash() * cashAllocationFraction;

        double maxCashPerAllocation = portfolio.cash() * maxAllocationFractionPerTicker;

        if (cashToDeploy <= 0.0) {
            return List.of();
        }

        List<ScoredPrediction> scoredPredictions =
                candidates.stream()
                        .map(prediction -> new ScoredPrediction(
                                prediction,
                                score(prediction)
                        ))
                        .filter(scored -> scored.score() > 0.0)
                        .sorted(Comparator.comparingDouble(
                                ScoredPrediction::score
                        ).reversed())
                        .toList();

        double totalScore = scoredPredictions.stream()
                .mapToDouble(ScoredPrediction::score)
                .sum();

        if (totalScore <= 0.0) {
            return List.of();
        }

        List<CapitalAllocation> allocations = new ArrayList<>();

        for (ScoredPrediction scoredPrediction : scoredPredictions) {
            double allocationWeight = scoredPrediction.score() / totalScore;
            double allocatedCash = Math.min(
                    cashToDeploy * allocationWeight,
                    maxCashPerAllocation
            );

            allocations.add(new CapitalAllocation(
                    scoredPrediction.prediction().getTicker(),
                    allocatedCash,
                    cycleTime,
                    cycleTime.plus(DEFAULT_HOLDING_PERIOD),
                    strategyId
            ));
        }

        return allocations;
    }

    //TODO: use volatility statistics (mean/std) for scoring
    private double score(ClassificationWithVolatilityPrediction prediction) {
        double probabilityEdge =
                prediction.probability() - minProbabilityThreshold;

        double volatilityPenalty =
                1.0 + Math.max(0.0, prediction.volatility());

        return probabilityEdge / volatilityPenalty;
    }

    private record ScoredPrediction(
            ClassificationWithVolatilityPrediction prediction,
            double score
    ) {
    }
}
