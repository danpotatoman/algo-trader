package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Represents the combined result of a model prediction on a specific data batch.
 *
 * <p>A {@code PredictionResult} bundles:
 * <ul>
 *     <li>The input {@link DataBatch} used for the prediction</li>
 *     <li>The resulting {@link PredictionScore} produced by the model</li>
 * </ul>
 *
 * <p>This class serves as the interface between the model layer and the
 * strategy layer (e.g. {@code PredictionInterpreter}). It provides convenient
 * access to both raw data context and prediction metadata.
 *
 * <p>This class is immutable.
 */
public final class ClassificationPrediction implements ModelPrediction {

    private final DataBatch batch;
    private final ClassificationScore score;

    /**
     * Constructs a {@code PredictionResult}.
     *
     * @param batch the input data batch used for the prediction
     * @param score the prediction score produced by the model
     *
     * @throws IllegalArgumentException if either argument is null
     *                                  or if ticker mismatch occurs
     */
    public ClassificationPrediction(DataBatch batch, ClassificationScore score) {
        if (batch == null) {
            throw new IllegalArgumentException("DataBatch cannot be null");
        }
        if (score == null) {
            throw new IllegalArgumentException("PredictionScore cannot be null");
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
     * Returns the underlying {@link PredictionScore}.
     *
     * @return the prediction score
     */
    public ClassificationScore getScore() {
        return score;
    }

    /**
     * Returns the ticker associated with this prediction.
     *
     * @return the ticker symbol, or null if the batch is empty
     */
    public String getTicker() {
        return score.getTicker();
    }

    /**
     * Returns the prediction value (e.g. 0 = down, 1 = up).
     *
     * @return the predicted class
     */
    public int getPrediction() {
        return score.getPrediction();
    }

    /**
     * Returns the confidence of the prediction.
     *
     * @return the confidence score in [0.0, 1.0]
     */
    public double getConfidence() {
        return score.getConfidence();
    }

    /**
     * Returns the prediction label.
     *
     * @return the human-readable label
     */
    public String getLabel() {
        return score.getLabel();
    }

    /**
     * Returns whether the model predicts upward movement.
     *
     * @return true if prediction == 1
     */
    public boolean predictsUp() {
        return score.predictsUp();
    }

    /**
     * Returns whether the model predicts downward movement.
     *
     * @return true if prediction == 0
     */
    public boolean predictsDown() {
        return score.predictsDown();
    }

    /**
     * Returns the timestamp of the final candle in the batch.
     *
     * @return the batch's final timestamp, or null if empty
     */
    public Instant getFinalTimestamp() {
        return batch.getFinalTimestamp();
    }

    /**
     * Returns the size of the batch used for the prediction.
     *
     * @return the number of rows in the batch
     */
    public int getBatchSize() {
        return batch.getBatchSize();
    }

    /**
     * Returns the time interval associated with the underlying data batch.
     *
     * @return the batch time interval
     */
    public TimeInterval getInterval() {
        return batch.getInterval();
    }

     @Override
    public String summary() {
        return "ClassificationPrediction{" +
                "ticker=" + getTicker() +
                ", prediction=" + score.getLabel() +
                ", confidence=" + String.format("%.2f%%", score.getConfidence() * 100.0) +
                '}';
    }
}