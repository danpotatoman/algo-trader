package com.algotrader.decision.interpreter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Volatility-aware classification strategy that enters a position when upward
 * probability is high enough and forecast volatility is low enough.
 *
 * <p>This interpreter generates a single round-trip trade when the prediction
 * probability meets the configured confidence threshold and volatility does
 * not exceed the configured maximum.
 */
public final class ClassificationWithVolatilityTradePlanner
        implements TradePlanner<ClassificationWithVolatilityPrediction> {

    private static final int FIXED_QUANTITY = 1;

    private final double minimumConfidenceThreshold;
    private final double maximumVolatilityThreshold;
    private final String strategyId;

    /**
     * Creates a volatility-aware classification interpreter.
     *
     * @param minimumConfidenceThreshold minimum upward probability required
     *        before a trade is generated
     * @param maximumVolatilityThreshold maximum forecast volatility allowed
     *        before a trade is generated
     * @param strategyId identifier associated with generated trades
     * @throws IllegalArgumentException if any argument is invalid
     */
    public ClassificationWithVolatilityTradePlanner(
            double minimumConfidenceThreshold,
            double maximumVolatilityThreshold,
            String strategyId
    ) {
        validateProbabilityThreshold(minimumConfidenceThreshold);
        validateVolatilityThreshold(maximumVolatilityThreshold);
        validateStrategyId(strategyId);

        this.minimumConfidenceThreshold = minimumConfidenceThreshold;
        this.maximumVolatilityThreshold = maximumVolatilityThreshold;
        this.strategyId = strategyId;
    }

    /**
     * Generates a trade when confidence and volatility satisfy the configured
     * thresholds.
     *
     * @param prediction classification-with-volatility prediction to interpret
     * @return a single trade if the prediction satisfies the strategy rules;
     *         otherwise an empty list
     * @throws IllegalArgumentException if {@code prediction} is null
     */
    @Override
    public List<RoundTripTrade> getTrades(
            ClassificationWithVolatilityPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "ClassificationWithVolatilityPrediction cannot be null."
            );
        }

        double probability = prediction.getProbability();
        double volatility = prediction.getVolatility();
        int horizonMinutes = prediction.getHorizonMinutes();

        if (probability < minimumConfidenceThreshold) {
            return List.of();
        }

        if (volatility > maximumVolatilityThreshold) {
            return List.of();
        }

        Instant entryTime = prediction.getLastCandleCloseTimestamp();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(horizonMinutes));

        RoundTripTrade trade = new RoundTripTrade(
                prediction.getTicker(),
                FIXED_QUANTITY,
                entryTime,
                exitTime,
                probability,
                strategyId
        );

        return List.of(trade);
    }

    private static void validateProbabilityThreshold(
            double minimumConfidenceThreshold
    ) {
        if (Double.isNaN(minimumConfidenceThreshold)
                || Double.isInfinite(minimumConfidenceThreshold)) {
            throw new IllegalArgumentException(
                    "minimumConfidenceThreshold must be finite."
            );
        }

        if (minimumConfidenceThreshold < 0.0
                || minimumConfidenceThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "minimumConfidenceThreshold must be between 0.0 and 1.0."
            );
        }
    }

    private static void validateVolatilityThreshold(
            double maximumVolatilityThreshold
    ) {
        if (Double.isNaN(maximumVolatilityThreshold)
                || Double.isInfinite(maximumVolatilityThreshold)) {
            throw new IllegalArgumentException(
                    "maximumVolatilityThreshold must be finite."
            );
        }

        if (maximumVolatilityThreshold < 0.0) {
            throw new IllegalArgumentException(
                    "maximumVolatilityThreshold must be non-negative."
            );
        }
    }

    private static void validateStrategyId(String strategyId) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "strategyId cannot be null or blank."
            );
        }
    }
}
