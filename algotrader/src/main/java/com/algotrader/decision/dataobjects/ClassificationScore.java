package com.algotrader.decision.dataobjects;

/**
 * Represents the prediction output from a machine learning model for a given ticker.
 *
 * <p>A {@code ClassificationScore} encapsulates:
 * <ul>
 *     <li>The ticker symbol associated with the prediction</li>
 *     <li>A binary prediction (e.g. 0 = down, 1 = up)</li>
 *     <li>A confidence score in the prediction</li>
 *     <li>A human-readable label describing the prediction</li>
 * </ul>
 *
 * <p>This class is immutable and validated upon construction to ensure all fields
 * are consistent and within expected ranges.
 *
 * <p>It is typically produced by the model API layer and consumed by a
 * trading component such as {@code Trader}.
 */
public final class ClassificationScore {

    /**
     * The stock ticker symbol associated with this prediction.
     */
    private final String ticker;

    /**
     * The predicted class (e.g. 0 = down, 1 = up).
     */
    private final int prediction;

    /**
     * Confidence score for the prediction, typically in the range [0.0, 1.0].
     */
    private final double confidence;

    /**
     * Human-readable label describing the prediction (e.g. "UP", "DOWN").
     */
    private final String label;

    /**
     * Constructs a new {@code ClassificationScore}.
     *
     * @param ticker the stock ticker symbol
     * @param prediction the predicted class (0 or 1)
     * @param confidence the confidence score (must be between 0.0 and 1.0)
     * @param label a human-readable label describing the prediction
     *
     * @throws IllegalArgumentException if any argument is invalid:
     * <ul>
     *     <li>{@code ticker} is null or blank</li>
     *     <li>{@code prediction} is not 0 or 1</li>
     *     <li>{@code confidence} is not in [0.0, 1.0]</li>
     *     <li>{@code label} is null or blank</li>
     * </ul>
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
     * Validates the fields used to construct a {@code ClassificationScore}.
     *
     * @param ticker the ticker symbol
     * @param prediction the predicted class
     * @param confidence the confidence score
     * @param label the prediction label
     *
     * @throws IllegalArgumentException if any value is invalid
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

    /**
     * Returns the ticker symbol associated with this prediction.
     *
     * @return the ticker symbol
     */
    public String getTicker() {
        return ticker;
    }

    /**
     * Returns the predicted class.
     *
     * @return the prediction (0 = down, 1 = up)
     */
    public int getPrediction() {
        return prediction;
    }

    /**
     * Returns the confidence score for the prediction.
     *
     * @return the confidence value in the range [0.0, 1.0]
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Returns the human-readable label describing the prediction.
     *
     * @return the prediction label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns whether the prediction indicates an upward movement.
     *
     * @return {@code true} if prediction == 1, otherwise {@code false}
     */
    public boolean predictsUp() {
        return prediction == 1;
    }

    /**
     * Returns whether the prediction indicates a downward movement.
     *
     * @return {@code true} if prediction == 0, otherwise {@code false}
     */
    public boolean predictsDown() {
        return prediction == 0;
    }

    /**
     * Returns a human-readable string representation of this prediction.
     *
     * <p>Includes ticker, prediction, label, and confidence formatted as a percentage.
     *
     * @return a formatted string representation of this {@code ClassificationScore}
     */
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