package com.algotrader.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects trading cycle logs for a multi-ticker trading session.
 *
 * <p>A logger instance is intended for a single trading session. Call
 * {@link #start()} before logging cycles, then call
 * {@link #finish()} once the session has completed to obtain the assembled
 * {@link TradingSessionLog}.
 */
public final class MultiTickerTradingSessionLogger {

    private Instant startTime;

    private final List<MultiTickerTradingCycleLog> cycleLogs =
            new ArrayList<>();

    /**
     * Starts a new trading session.
     *
     * @throws IllegalStateException if a session has already been started
     */
    public void start() {
        if (startTime != null) {
            throw new IllegalStateException(
                    "Trading session has already been started.");
        }

        startTime = Instant.now();
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
     * Finishes the current trading session and returns its log.
     *
     * @return completed trading session log
     * @throws IllegalStateException if the session has not been started
     */
    public TradingSessionLog finish() {
        return finish(null);
    }

    /**
     * Finishes the current trading session and returns its log.
     *
     * @return completed trading session log
     * @throws IllegalStateException if the session has not been started
     */
    public TradingSessionLog finish(Exception sessionFailure) {
        if (startTime == null) {
            throw new IllegalStateException(
                    "Trading session has not been started.");
        }

        TradingSessionLog sessionLog = new TradingSessionLog(
                startTime,
                Instant.now(),
                List.copyOf(cycleLogs),
                sessionFailure
        );

        startTime = null;
        cycleLogs.clear();

        return sessionLog;
    }
}