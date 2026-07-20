package com.algotrader.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects trading cycle logs for a multi-ticker trading session.
 *
 * <p>A logger instance is intended for a single trading session. Call
 * {@link #start()} before logging cycles, then call
 * {@link #finish(double)} or {@link #finish(double, Exception)} once the
 * session has completed to obtain the assembled {@link TradingSessionLog}.
 */
public final class MultiTickerTradingSessionLogger {

    private Instant startTime;
    private double startingCash;
    private ForcedLiquidationResult liquidationResult = null;

    private final List<MultiTickerTradingCycleLog> cycleLogs =
            new ArrayList<>();

    /**
     * Starts a new trading session.
     *
     * @throws IllegalStateException if a session has already been started
     */
    public void start(double startingCash) {
        if (startTime != null) {
            throw new IllegalStateException(
                    "Trading session has already been started.");
        }

        startTime = Instant.now();
        this.startingCash = startingCash;
        cycleLogs.clear();
    }

    /**
     * Adds a trading cycle log to the current session.
     *
     * @param cycleLog cycle log to record
     * @throws IllegalArgumentException if {@code cycleLog} is null
     * @throws IllegalStateException if the session has not been started
     */
    public void logCycle(MultiTickerTradingCycleLog cycleLog) {
        if (startTime == null) {
            throw new IllegalStateException(
                    "Trading session has not been started.");
        }

        if (cycleLog == null) {
            throw new IllegalArgumentException(
                    "Cycle log cannot be null.");
        }

        cycleLogs.add(cycleLog);
    }

    /**
     * Records the end-of-session liquidation result for the current session.
     *
     * @param liquidationResult forced liquidation result to include in the
     *        session log
     * @throws IllegalStateException if liquidation has already been logged
     */
    public void logEndOfSessionLiquidation(ForcedLiquidationResult liquidationResult) {
        if (this.liquidationResult != null) {
            throw new IllegalStateException("logEndOfSessionLiquidation() can only be called once.");
        }

        this.liquidationResult = liquidationResult;
    }

    /**
     * Finishes the current trading session and returns its log.
     *
     * @param endingCash cash balance at session completion
     * @return completed trading session log
     * @throws IllegalStateException if the session has not been started
     */
    public TradingSessionLog finish(double endingCash) {
        return finish(endingCash, null);
    }

    /**
     * Finishes the current trading session and returns its log.
     *
     * @param endingCash cash balance at session completion
     * @param sessionFailure exception that ended the session, or null if the
     *        session completed normally
     * @return completed trading session log
     * @throws IllegalStateException if the session has not been started
     */
    public TradingSessionLog finish(
        double endingCash,
        Exception sessionFailure) {
        if (startTime == null) {
            throw new IllegalStateException(
                    "Trading session has not been started.");
        }

        TradingSessionLog sessionLog = new TradingSessionLog(
                startTime,
                Instant.now(),//TODO: start and end times are for candle times used, not for how long a session took to run on my machine.
                startingCash,
                endingCash,
                List.copyOf(cycleLogs),
                liquidationResult,
                SessionFailureLog.from(sessionFailure)
        );

        startTime = null;
        cycleLogs.clear();

        return sessionLog;
    }
}
