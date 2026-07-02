package com.algotrader.execution;

import java.util.List;

import com.algotrader.decision.dataobjects.TradeInstruction;

/**
 * Responsible for executing or simulating trade instructions.
 *
 * <p>A {@code TradeExecutor} receives trade instructions and determines how
 * they should be processed. Implementations may:
 * <ul>
 *     <li>Execute trades immediately</li>
 *     <li>Schedule trades for future execution</li>
 *     <li>Simulate execution using historical market data</li>
 *     <li>Submit orders to a broker or exchange</li>
 *     <li>Reject or filter instructions based on execution constraints</li>
 * </ul>
 *
 * <p>This interface represents the execution layer of the trading pipeline:
 *
 * <pre>
 * TradeInstruction -> Execution -> TradeExecutionResult
 * </pre>
 *
 * <p>The interface does not prescribe how instructions are executed.
 * Different implementations may support paper trading, backtesting, live
 * broker integration, delayed execution, or other execution models.
 */
public interface TradeExecutor {

    /**
     * Handles a single trade instruction.
     *
     * <p>The instruction may be executed immediately, scheduled for later
     * execution, simulated, or ignored depending on the implementation.
     *
     * @param instruction trade instruction to handle
     * @return the execution result
     * @throws IllegalArgumentException if {@code instruction} is null
     */
    TradeExecutionResult handleInstruction(
            TradeInstruction instruction
    );

    /**
     * Handles a collection of trade instructions.
     *
     * <p>The default implementation executes each instruction in order by
     * delegating to {@link #handleInstruction(TradeInstruction)}. Implementations
     * may override this method to perform more efficient batch execution.
     *
     * @param instructions trade instructions to handle
     * @return execution results corresponding to each instruction, in order
     * @throws IllegalArgumentException if {@code instructions} is null or
     *         contains a null instruction
     */
    default List<TradeExecutionResult> handleInstructions(
            List<TradeInstruction> instructions
    ) {
        if (instructions == null) {
            throw new IllegalArgumentException(
                    "Trade instructions cannot be null.");
        }

        return instructions.stream()
                .map(instruction -> {
                    if (instruction == null) {
                        throw new IllegalArgumentException(
                                "Trade instructions cannot contain null.");
                    }

                    return handleInstruction(instruction);
                })
                .toList();
    }
}
