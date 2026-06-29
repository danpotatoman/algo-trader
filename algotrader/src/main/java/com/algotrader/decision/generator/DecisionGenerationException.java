package com.algotrader.decision.generator;

/**
 * Exception thrown when a {@link PortfolioDecisionGenerator} is unable to
 * produce a portfolio decision.
 *
 * <p>This exception represents failures that occur while evaluating market
 * data and portfolio state into a {@code PortfolioDecisionResult}. The
 * underlying cause may originate from prediction providers or other
 * dependencies used during decision generation.
 */
public class DecisionGenerationException extends Exception {

    /**
     * Creates a new exception with the given message.
     *
     * @param message description of the failure
     */
    public DecisionGenerationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message description of the failure
     * @param cause underlying cause of the failure
     */
    public DecisionGenerationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}