package com.algotrader.execution;

import com.algotrader.decision.dataobjects.PortfolioAction;
import com.algotrader.decision.dataobjects.TradeInstruction;

/**
 * Responsible for executing or simulating trade instructions.
 *
 * <p>A {@code TradeExecutor} receives a trade instruction and
 * determines how it should be processed. Implementations may:
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
 * TradeInstruction -> Execution -> PortfolioAction
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
     * @return a portfolio action describing the resulting execution
     * @throws IllegalArgumentException if {@code instruction} is null
     */
    PortfolioAction handleInstruction(
            TradeInstruction instruction
    );
}
