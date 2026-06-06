package com.algotrader.decision.dataobjects;

/**
 * Immutable binary classification result produced by a prediction model.
 *
 * <p>A {@code ClassificationScore} represents the model's prediction for a
 * single ticker symbol. The prediction is binary, where {@code 1} represents
 * an upward movement and {@code 0} represents a downward movement.
 *
 * <p>The confidence value represents the model's confidence in the predicted
 * class and must be in the range {@code [0.0, 1.0]}. The label provides a
 * human-readable description of the predicted class, such as {@code "UP"} or
 * {@code "DOWN"}.
 *
 * <p>This class is typically produced by the prediction provider layer and
 * consumed by trade generation components.
 */
public final class ClassificationScore {

    private final String ticker;
    private final int prediction;
    private final double confidence;
    private final String label;

    /**
     * Creates a classification score.
     *
     * @param ticker ticker symbol associated with the prediction
     * @param prediction predicted class, where {@code 1} means up and
     *        {@code 0} means down
     * @param confidence model confidence in the predicted class, in the range
     *        {@code [0.0, 1.0]}
     * @param label human-readable label describing the predicted class
     * @throws IllegalArgumentException if any argument is invalid
     */
    public ClassificationScore(
            String ticker,
            int prediction,
            double confidence,
            String label
    ) {
        validate(ticker, prediction, confidence, label);

        this.ticker = ticker;
        this.prediction = prediction;
        this.confidence = confidence;
        this.label = label;
    }

    /**
     * Validates constructor arguments before a classification score is created.
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    private void validate(
            String ticker,
            int prediction,
            double confidence,
            String label
    ) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be empty");
        }

        if (prediction != 0 && prediction != 1) {
            throw new IllegalArgumentException("Prediction must be 0 or 1");
        }

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }
    }

    public String getTicker() {
        return ticker;
    }

    public int getPrediction() {
        return prediction;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns whether the prediction indicates upward movement.
     *
     * @return {@code true} if the predicted class is {@code 1}
     */
    public boolean predictsUp() {
        return prediction == 1;
    }

    /**
     * Returns whether the prediction indicates downward movement.
     *
     * @return {@code true} if the predicted class is {@code 0}
     */
    public boolean predictsDown() {
        return prediction == 0;
    }

    @Override
    public String toString() {
        return String.format(
            "ClassificationScore[ticker=%s, prediction=%d (%s), confidence=%.2f%%]",
            ticker,
            prediction,
            label,
            confidence * 100.0
        );
    }
}