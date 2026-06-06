package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable classification prediction produced by a model for a specific
 * market data batch.
 *
 * <p>A {@code ClassificationPrediction} combines:
 * <ul>
 *     <li>The {@link DataBatch} used as model input</li>
 *     <li>The resulting {@link ClassificationScore} produced by the model</li>
 * </ul>
 *
 * <p>This class serves as the primary output of the classification prediction
 * layer. It preserves both the prediction result and the market data context
 * from which that prediction was generated.
 *
 * <p>The prediction and batch are required to reference the same ticker
 * symbol, ensuring that prediction results remain consistent with their
 * underlying input data.
 */
public final class ClassificationPrediction implements ModelPrediction {

    private final DataBatch batch;
    private final ClassificationScore score;

    /**
     * Creates a classification prediction from a model input batch and
     * classification score.
     *
     * @param batch the market data batch used as model input
     * @param score the classification score produced by the model
     * @throws IllegalArgumentException if either argument is null or if the
     *         batch and score refer to different ticker symbols
     */
    public ClassificationPrediction(DataBatch batch, ClassificationScore score) {
        if (batch == null) {
            throw new IllegalArgumentException("DataBatch cannot be null");
        }
        if (score == null) {
            throw new IllegalArgumentException("ClassificationScore cannot be null");
        }

        String batchTicker = batch.getTicker();
        String scoreTicker = score.getTicker();

        if (batchTicker != null && !batchTicker.equals(scoreTicker)) {
            throw new IllegalArgumentException(
                    "Ticker mismatch between batch (" + batchTicker +
                    ") and score (" + scoreTicker + ")"
            );
        }

        this.batch = batch;
        this.score = score;
    }

    /**
     * Returns the underlying {@link DataBatch}.
     *
     * @return the data batch used for the prediction
     */
    public DataBatch getBatch() {
        return batch;
    }

    /**
     * Returns the underlying {@link ClassificationScore}.
     *
     * @return the prediction score
     */
    public ClassificationScore getScore() {
        return score;
    }

    public String getTicker() {
        return score.getTicker();
    }

    public int getPrediction() {
        return score.getPrediction();
    }

    public double getConfidence() {
        return score.getConfidence();
    }

    public String getLabel() {
        return score.getLabel();
    }

    public boolean predictsUp() {
        return score.predictsUp();
    }

    public boolean predictsDown() {
        return score.predictsDown();
    }

    public Instant getFinalTimestamp() {
        return batch.getFinalTimestamp();
    }

    public int getBatchSize() {
        return batch.getBatchSize();
    }

    public TimeInterval getInterval() {
        return batch.getInterval();
    }

    /**
     * Returns a concise human-readable description of this prediction suitable
     * for logging and diagnostic output.
     *
     * @return a summary of the prediction result
     */
    @Override
    public String summary() {
        return "ClassificationPrediction{" +
                "ticker=" + getTicker() +
                ", prediction=" + score.getLabel() +
                ", confidence=" + String.format("%.2f%%", score.getConfidence() * 100.0) +
                '}';
    }
}