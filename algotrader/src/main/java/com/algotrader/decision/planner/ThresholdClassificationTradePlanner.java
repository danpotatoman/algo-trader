package com.algotrader.decision.planner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.algotrader.decision.dataobjects.ClassificationPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Classification-based trading strategy that enters a position when the
 * model predicts a sufficiently confident upward move.
 *
 * <p>This planner generates a single round-trip trade when:
 * <ul>
 *     <li>The model predicts the positive class</li>
 *     <li>The prediction confidence is at least the configured threshold
 *         {@code k}</li>
 * </ul>
 *
 * <p>When a trade is generated, the entry time is the timestamp of the
 * final candle in the prediction batch and the exit time is calculated by
 * adding the configured forecast horizon.
 *
 * <p>No trade is generated when the model predicts the negative class or
 * when confidence falls below the configured threshold.
 *
 * <p>This strategy was developed for the project's original classification
 * pipeline and serves as a simple baseline for converting binary model
 * predictions into trading decisions.
 */
public final class ThresholdClassificationTradePlanner
        implements TradePlanner<ClassificationPrediction> {

    private final double k;
    private final String strategyId;

    /**
     * Creates a threshold-based classification trading strategy.
     *
     * @param k minimum confidence required before a trade is generated
     * @param strategyId identifier associated with generated trades
     * @throws IllegalArgumentException if any argument is invalid
     */
    public ThresholdClassificationTradePlanner(
            double k,
            String strategyId
    ) {
        if (k < 0.0 || k > 1.0) {
            throw new IllegalArgumentException(
                    "Confidence threshold k must be between 0 and 1."
            );
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        this.k = k;
        this.strategyId = strategyId;
    }

    /**
     * Generates a trade when the prediction indicates an upward move with
     * sufficient confidence.
     *
     * <p>The generated trade enters when the final input candle has closed and
     * exits after the configured forecast horizon.
     *
     * @param prediction classification prediction to evaluate
     * @return a single trade if the prediction satisfies the strategy rules;
     *         otherwise an empty list
     * @throws IllegalArgumentException if {@code prediction} is null or its
     *         final timestamp is null
     */
    @Override
    public List<RoundTripTrade> getTrades(
            ClassificationPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "ClassificationPrediction cannot be null."
            );
        }

        if (!prediction.predictsUp()
                || prediction.getConfidence() < k) {
            return List.of();
        }

        String ticker = prediction.getTicker();
        Instant entryTime = prediction.getLastCandleCloseTimestamp();
        int horizonMinutes = prediction.getHorizonMinutes();

        if (entryTime == null) {
            throw new IllegalArgumentException(
                    "Prediction final timestamp cannot be null."
            );
        }

        Instant exitTime = entryTime.plus(
                horizonMinutes,
                ChronoUnit.MINUTES
        );

        RoundTripTrade trade = new RoundTripTrade(
                ticker,
                1,// TODO: Replace fixed quantity sizing with a dedicated position-sizing mechanism once trade sizing is separated from signal generation.
                entryTime,
                exitTime,
                strategyId
        );

        return List.of(trade);
    }
}
