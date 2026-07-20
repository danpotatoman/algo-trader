package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.execution.FailedTradeExecution;
import com.algotrader.execution.TradeExecutionResult;

/**
 * Summarizes the results of the end-of-session liquidation phase.
 *
 * <p>This record captures all trades that required forced liquidation after
 * the normal trading cycles completed, together with the outcome of each
 * liquidation attempt.
 *
 * @param liquidationTime timestamp at which forced liquidation was attempted
 * @param attemptedTrades trades that were still open and required liquidation
 * @param successfulExecutions successful liquidation executions
 * @param failedExecutions failed liquidation executions
 * @param registryEmptyAfter {@code true} if the open-trade registry was empty
 *        after liquidation completed; {@code false} otherwise
 */
public record ForcedLiquidationResult(
        Instant liquidationTime,
        List<RoundTripTrade> attemptedTrades,
        List<TradeExecutionResult> successfulExecutions,
        List<FailedTradeExecution> failedExecutions,
        boolean registryEmptyAfter
) {

    /**
     * Creates a forced liquidation result.
     *
     * @throws IllegalArgumentException if any required field is null or if the
     *         execution results exceed the number of attempted trades
     */
    public ForcedLiquidationResult {
        if (liquidationTime == null) {
            throw new IllegalArgumentException(
                    "liquidationTime cannot be null."
            );
        }

        if (attemptedTrades == null) {
            throw new IllegalArgumentException(
                    "attemptedTrades cannot be null."
            );
        }

        if (successfulExecutions == null) {
            throw new IllegalArgumentException(
                    "successfulExecutions cannot be null."
            );
        }

        if (failedExecutions == null) {
            throw new IllegalArgumentException(
                    "failedExecutions cannot be null."
            );
        }

        attemptedTrades = List.copyOf(attemptedTrades);
        successfulExecutions = List.copyOf(successfulExecutions);
        failedExecutions = List.copyOf(failedExecutions);

        if (successfulExecutions.size() + failedExecutions.size()
                > attemptedTrades.size()) {
            throw new IllegalArgumentException(
                    "More execution results than attempted trades."
            );
        }
    }
}