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

    /**
     * Returns the source market data batch.
     *
     * @return source market data batch
     */
    public DataBatch getBatch() {
        return batch;
    }

    /**
     * Returns the upward classification probability.
     *
     * @return probability from {@code 0.0} through {@code 1.0}
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Returns the forecast volatility used for risk-aware decisions.
     *
     * @return non-negative forecast volatility
     */
    public double getVolatility() {
        return volatility;
    }

    /**
     * Returns the prediction horizon.
     *
     * @return prediction horizon in minutes
     */
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
                        "ticker=%s, timestamp=%s, interval=%s, " +
                        "probability=%.4f, volatility=%.6f, " +
                        "horizonMinutes=%d]",
                getTicker(),
                getFinalTimestamp(),
                getInterval(),
                probability,
                volatility,
                horizonMinutes
        );
    }

    /**
     * Returns the ticker symbol from the source batch.
     *
     * @return source batch ticker symbol
     */
    @Override
    public String getTicker() {
        return batch.getTicker();
    }

    /**
     * Returns the final timestamp in the source market data batch.
     *
     * @return final batch timestamp
     */
    @Override
    public Instant getFinalTimestamp() {
        return batch.getFinalTimestamp();
    }

    /**
     * Returns the market data interval used by the source batch.
     *
     * @return source batch interval
     */
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
