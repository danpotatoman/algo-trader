package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable classification prediction produced for a market data batch.
 *
 * <p>This prediction type is supported end-to-end by the trading workflow.
 * The numeric prediction uses {@code 1} for an upward classification and
 * {@code 0} for a downward classification; {@code label} provides the
 * corresponding human-readable model output.
 */
public final class ClassificationPrediction implements ModelPrediction {

    private final DataBatch batch;
    private final String ticker;
    private final int prediction;
    private final double confidence;
    private final int horizonMinutes;
    private final String label;

    /**
     * Creates a validated classification prediction for a data batch.
     *
     * @param batch source market data batch
     * @param ticker ticker symbol associated with the prediction
     * @param prediction numeric class, where {@code 1} means up and
     *        {@code 0} means down
     * @param confidence model confidence between {@code 0.0} and {@code 1.0}
     * @param horizonMinutes prediction horizon in minutes
     * @param label human-readable prediction label
     * @throws IllegalArgumentException if any argument is invalid or the
     *         ticker does not match the batch ticker
     */
    public ClassificationPrediction(
            DataBatch batch,
            String ticker,
            int prediction,
            double confidence,
            int horizonMinutes,
            String label
    ) {
        validate(batch, ticker, prediction, confidence, horizonMinutes, label);

        if (!batch.getTicker().equals(ticker)) {
            throw new IllegalArgumentException(
                    "Ticker mismatch between batch (" + batch.getTicker()
                            + ") and prediction (" + ticker + ")"
            );
        }

        this.batch = batch;
        this.ticker = ticker;
        this.prediction = prediction;
        this.confidence = confidence;
        this.horizonMinutes = horizonMinutes;
        this.label = label;
    }

    public DataBatch getBatch() {
        return batch;
    }

    @Override
    public String getTicker() {
        return ticker;
    }

    public int getPrediction() {
        return prediction;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getHorizonMinutes() {
        return horizonMinutes;
    }

    public String getLabel() {
        return label;
    }

    public boolean predictsUp() {
        return prediction == 1;
    }

    public boolean predictsDown() {
        return prediction == 0;
    }

    @Override
    public Instant getLastCandleCloseTimestamp() {
        return batch.getLastCandleTimestamp();
    }

    public int getBatchSize() {
        return batch.getBatchSize();
    }

    @Override
    public TimeInterval getInterval() {
        return batch.getInterval();
    }

    /**
     * Returns a concise human-readable prediction summary.
     *
     * @return prediction summary
     */
    @Override
    public String summary() {
        return "ClassificationPrediction{" +
                "ticker=" + ticker +
                ", prediction=" + label +
                ", confidence=" + String.format("%.2f%%", confidence * 100.0) +
                ", horizonMinutes=" + horizonMinutes +
                '}';
    }

    private static void validate(
            DataBatch batch,
            String ticker,
            int prediction,
            double confidence,
            int horizonMinutes,
            String label
    ) {
        if (batch == null) {
            throw new IllegalArgumentException("DataBatch cannot be null");
        }

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank");
        }

        if (prediction != 0 && prediction != 1) {
            throw new IllegalArgumentException("Prediction must be 0 or 1");
        }

        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            throw new IllegalArgumentException("Confidence must be finite");
        }

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    "Confidence must be between 0.0 and 1.0"
            );
        }

        if (horizonMinutes <= 0) {
            throw new IllegalArgumentException(
                    "horizonMinutes must be positive"
            );
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label cannot be null or blank");
        }
    }
}
