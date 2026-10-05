package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

/**
 * Complete log of a trading session.
 *
 * <p>A trading session consists of one or more trading cycles executed by a
 * trading driver. This record captures the lifecycle of the session together
 * with the log produced for each evaluated cycle.
 *
 * <p>The log is intentionally a faithful record of what occurred during the
 * sessuib rather than a collection of derived performance metrics. Such metrics
 * may be computed later by analytics components.
 */
public record TradingSessionLog(

        /**
         * Time at which the trading session began.
         */
        Instant startTime,

        /**
         * Time at which the trading session completed.
         */
        Instant endTime,

        /**
         * Portfolio cash available when the session began.
         */
        double startingCash,

        /**
         * Portfolio cash available when the session completed.
         */
        double endingCash,

        /**
         * Log for each evaluated trading cycle, in chronological order.
         */
        List<MultiTickerTradingCycleLog> cycleLogs,

        /**
         * Result for attempted end-of-session liquidation of remaining open positions.
         */
        ForcedLiquidationResult liquidationResult,

        /**
         * Exception that caused the session to fail, null if session succeeded.
         */
        SessionFailureLog sessionFailure,
        String schemaVersion,
        String runId,
        com.algotrader.portfolio.PortfolioValuation initialPortfolio,
        com.algotrader.portfolio.PortfolioValuation finalPortfolio,
        List<com.algotrader.decision.dataobjects.RoundTripTrade> remainingOpenTrades

) {

    /**
     * Creates a trading session log.
     *
     * @param startTime session start time
     * @param endTime session end time
     * @param cycleLogs cycle logs in chronological order
     * @throws IllegalArgumentException if any required argument is invalid
     */
    public TradingSessionLog {
        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time cannot be null.");
        }

        if (endTime == null) {
            throw new IllegalArgumentException(
                    "End time cannot be null.");
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time.");
        }

        if (cycleLogs == null) {
            throw new IllegalArgumentException(
                    "Cycle logs cannot be null.");
        }

        if (startingCash < 0.0) {
            throw new IllegalArgumentException(
                    "Starting cash cannot be negative.");
        }

        if (endingCash < 0.0) {
            throw new IllegalArgumentException(
                    "Ending cash cannot be negative.");
        }

        cycleLogs = List.copyOf(cycleLogs);
    }
}
