package com.algotrader.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Iterates over a fixed range of historical trading cycle timestamps.
 *
 * <p>The clock begins at the supplied start time and advances by the supplied
 * {@link TimeInterval} until the end time is reached. Both the start and end
 * times are inclusive.
 *
 * <p>This class is intended to drive historical backtests by supplying the
 * timestamp for each trading cycle. It is deliberately independent of market
 * data availability.
 */
public final class HistoricalCycleClock {

    private final Instant endTime;
    private final Duration step;

    private Instant nextTime;

    /**
     * Creates a historical cycle clock.
     *
     * @param startTime first cycle timestamp (inclusive)
     * @param endTime final cycle timestamp (inclusive)
     * @param interval time between successive cycles
     * @throws IllegalArgumentException if any argument is null or if
     *         {@code startTime} is after {@code endTime}
     */
    public HistoricalCycleClock(
            Instant startTime,
            Instant endTime,
            TimeInterval interval) {

        if (startTime == null) {
            throw new IllegalArgumentException(
                    "Start time cannot be null.");
        }

        if (endTime == null) {
            throw new IllegalArgumentException(
                    "End time cannot be null.");
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "Time interval cannot be null.");
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must not be after end time.");
        }

        this.endTime = endTime;
        this.step = interval.getDuration();
        this.nextTime = startTime;
    }

    /**
     * Returns whether another cycle timestamp is available.
     *
     * @return {@code true} if another timestamp can be retrieved
     */
    public boolean hasNext() {
        return nextTime != null;
    }

    /**
     * Returns the next historical cycle timestamp.
     *
     * @return next cycle timestamp
     * @throws NoSuchElementException if no further timestamps remain
     */
    public Instant next() {
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "No further historical cycle timestamps remain.");
        }

        Instant current = nextTime;

    // TODO: Advance to the next timestamp that falls within regular market
    // hours. The current implementation advances by a fixed interval and
    // therefore emits timestamps corresponding to nights, weekends, and
    // market holidays.
        Instant candidate = current.plus(step);

        nextTime = candidate.isAfter(endTime)
                ? null
                : candidate;

        return current;
    }
}