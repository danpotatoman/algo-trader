package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable classification prediction with an accompanying volatility forecast.
 *
 * <p>This prediction type is supported end-to-end by the trading workflow. The
 * probability represents the model's upward classification confidence, while
 * volatility provides the risk metric used by volatility-aware strategies.
 */
public final class ClassificationWithVolatilityPrediction
        implements ModelPrediction {

    private final DataBatch batch;
    private final double probability;
    private final double volatility;
    private final int horizonMinutes;

    /**
     * Creates a validated classification-with-volatility prediction.
     *
     * @param batch source market data batch
     * @param probability upward classification probability from {@code 0.0}
     *        through {@code 1.0}
     * @param volatility non-negative forecast volatility
     * @param horizonMinutes prediction horizon in minutes
     * @throws IllegalArgumentException if any argument is invalid
     */
    public ClassificationWithVolatilityPrediction(
            DataBatch batch,
            double probability,
            double volatility,
            int horizonMinutes
    ) {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "batch cannot be null"
            );
        }

        validateProbability(probability);
        validateVolatility(volatility);
        validateHorizonMinutes(horizonMinutes);

        this.batch = batch;
        this.probability = probability;
        this.volatility = volatility;
        this.horizonMinutes = horizonMinutes;
    }

    public DataBatch getBatch() {
        return batch;
    }

    public double getProbability() {
        return probability;
    }

    public double getVolatility() {
        return volatility;
    }

    public int getHorizonMinutes() {
        return horizonMinutes;
    }

    /**
     * Returns a concise human-readable prediction summary.
     *
     * @return prediction summary
     */
    @Override
    public String summary() {
        return String.format(
                "ClassificationWithVolatilityPrediction[" +
                        "ticker=%s, lastCandleCloseTimestamp=%s, interval=%s, " +
                        "probability=%.4f, volatility=%.6f, " +
                        "horizonMinutes=%d]",
                getTicker(),
                getLastCandleCloseTimestamp(),
                getInterval(),
                probability,
                volatility,
                horizonMinutes
        );
    }

    @Override
    public String getTicker() {
        return batch.getTicker();
    }

    @Override
    public Instant getLastCandleCloseTimestamp() {
        return batch.getLastCandleTimestamp();
    }

    @Override
    public TimeInterval getInterval() {
        return batch.getInterval();
    }

    private static void validateProbability(double probability) {
        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            throw new IllegalArgumentException(
                    "probability must be finite"
            );
        }

        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException(
                    "probability must be between 0.0 and 1.0"
            );
        }
    }

    private static void validateVolatility(double volatility) {
        if (Double.isNaN(volatility) || Double.isInfinite(volatility)) {
            throw new IllegalArgumentException(
                    "volatility must be finite"
            );
        }

        if (volatility < 0.0) {
            throw new IllegalArgumentException(
                    "volatility must be non-negative"
            );
        }
    }

    private static void validateHorizonMinutes(int horizonMinutes) {
        if (horizonMinutes <= 0) {
            throw new IllegalArgumentException(
                    "horizonMinutes must be positive"
            );
        }
    }
}
