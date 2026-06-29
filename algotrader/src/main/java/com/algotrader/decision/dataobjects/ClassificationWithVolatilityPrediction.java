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
public record ClassificationWithVolatilityPrediction(
        DataBatch batch,
        double probability,
        double volatility,
        int horizonMinutes
) implements ModelPrediction {

    public ClassificationWithVolatilityPrediction {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "batch cannot be null"
            );
        }

        validateProbability(probability);
        validateVolatility(volatility);
        validateHorizonMinutes(horizonMinutes);
    }

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