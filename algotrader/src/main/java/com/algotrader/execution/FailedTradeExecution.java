package com.algotrader.execution;

import com.algotrader.decision.dataobjects.TradeInstruction;

/**
 * Records a trade execution that failed.
 *
 * <p>This record captures the instruction that was attempted together with a
 * description of the failure. It is intended for logging and analytics rather
 * than exception propagation.
 *
 * @param instruction trade instruction that failed to execute
 * @param errorMessage human-readable description of the failure
 */
public record FailedTradeExecution(
        TradeInstruction instruction,
        String errorMessage
) {

    /**
     * Creates a failed trade execution record.
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    public FailedTradeExecution {
        if (instruction == null) {
            throw new IllegalArgumentException(
                    "instruction cannot be null."
            );
        }

        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "errorMessage cannot be null or blank."
            );
        }
    }

    /**
     * Creates a failed execution record from an attempted instruction and the
     * exception raised while executing it.
     *
     * @param instruction trade instruction that failed
     * @param exception exception raised by execution
     * @return failed execution record suitable for session logging
     * @throws IllegalArgumentException if {@code instruction} is null or the
     *         derived error message is blank
     */
    public static FailedTradeExecution from(
        TradeInstruction instruction,
        Exception exception
    ) {
        return new FailedTradeExecution(
                instruction,
                exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage()
        );
    }
    }
