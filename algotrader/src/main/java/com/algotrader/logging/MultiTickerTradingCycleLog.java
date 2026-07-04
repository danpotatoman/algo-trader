package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.CapitalAllocation;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.execution.TradeExecutionResult;
import com.algotrader.registry.OpenTradeAdjustment;

/**
 * Log of a multi-ticker trading cycle.
 *
 * <p>This record captures everything that occurred during one evaluation
 * timestamp, including trade exits, portfolio evaluation, new trade entries,
 * open-trade adjustments, and any failures encountered.
 */
public record MultiTickerTradingCycleLog(

        /**
         * Timestamp of the trading cycle.
         */
        Instant cycleTime,

        /**
         * Result produced by the portfolio decision generator.
         * May be {@code null} if evaluation failed.
         */
        MultiTickerCycleEvaluation evaluation,

        /**
         * Trades that were due for exit at the beginning of the cycle.
         */
        List<RoundTripTrade> tradesDueForExit,

        /**
         * Trades successfully closed during the cycle.
         */
        List<RoundTripTrade> successfullyClosedTrades,

        /**
         * Successful trade executions corresponding to exits.
         */
        List<TradeExecutionResult> successfulExitExecutions,

        /**
         * Exit executions that failed.
         */
        List<TradeExecutionFailureLog> failedExitExecutions,

        /**
         * Capital allocations produced by the decision generator.
         */
        List<CapitalAllocation> attemptedCapitalAllocations,

        /**
         * Trades successfully opened during the cycle.
         */
        List<RoundTripTrade> successfullyOpenedTrades,

        /**
         * Successful trade executions corresponding to entries.
         */
        List<TradeExecutionResult> successfulEntryExecutions,

        /**
         * Entry executions that failed.
         */
        List<TradeExecutionFailureLog> failedEntryExecutions,

        /**
         * Requested modifications to existing open trades.
         */
        List<OpenTradeAdjustment> appliedOpenTradeAdjustments,

        /**
         * Cycle-level failure, if one occurred outside of individual trade
         * execution failures.
         */
        CycleFailureLog cycleFailure
) {

    public MultiTickerTradingCycleLog {
        if (cycleTime == null) {
            throw new IllegalArgumentException(
                    "Cycle time cannot be null.");
        }

        if (tradesDueForExit == null) {
            throw new IllegalArgumentException(
                    "Trades due for exit cannot be null.");
        }

        if (successfullyClosedTrades == null) {
            throw new IllegalArgumentException(
                    "Successfully closed trades cannot be null.");
        }

        if (successfulExitExecutions == null) {
            throw new IllegalArgumentException(
                    "Successful exit executions cannot be null.");
        }

        if (failedExitExecutions == null) {
            throw new IllegalArgumentException(
                    "Failed exit executions cannot be null.");
        }

        if (attemptedCapitalAllocations == null) {
            throw new IllegalArgumentException(
                    "Attempted capital allocations cannot be null.");
        }

        if (successfullyOpenedTrades == null) {
            throw new IllegalArgumentException(
                    "Successfully opened trades cannot be null.");
        }

        if (successfulEntryExecutions == null) {
            throw new IllegalArgumentException(
                    "Successful entry executions cannot be null.");
        }

        if (failedEntryExecutions == null) {
            throw new IllegalArgumentException(
                    "Failed entry executions cannot be null.");
        }

        if (appliedOpenTradeAdjustments == null) {
            throw new IllegalArgumentException(
                    "Applied open trade adjustments cannot be null.");
        }
    }
}