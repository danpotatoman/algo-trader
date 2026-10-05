package com.algotrader.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.marketdata.model.TimeInterval;

/** Iterates candle-open execution times within inclusive bounds and the supplied market calendar. */
public final class HistoricalCycleClock {
    private final Instant startTime;
    private final Instant endTime;
    private final Duration step;
    private final TimeInterval interval;
    private final MarketCalendar calendar;
    private final int totalCycles;
    private Instant nextTime;

    public HistoricalCycleClock(Instant startTime, Instant endTime, TimeInterval interval,
            MarketCalendar calendar) {
        if (startTime == null || endTime == null || interval == null || calendar == null) {
            throw new IllegalArgumentException("Clock bounds, interval and calendar are required.");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must not be after end time.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.step = interval.getDuration();
        this.calendar = calendar;
        if (!aligned(startTime) || !aligned(endTime)) {
            throw new IllegalArgumentException("Historical cycle bounds must align to " + interval + " candle opens.");
        }
        this.nextTime = nextExecutable(startTime);
        int count = 0;
        for (Instant time = nextTime; time != null; time = nextExecutable(time.plus(step))) {
            count = Math.incrementExact(count);
        }
        this.totalCycles = count;
    }

    private boolean aligned(Instant time) {
        return time.getNano() == 0 && Math.floorMod(time.getEpochSecond(), step.toSeconds()) == 0;
    }

    private Instant nextExecutable(Instant candidate) {
        while (!candidate.isAfter(endTime)) {
            if (calendar.isTradingTime(candidate)
                    && !candidate.isAfter(calendar.getLastExecutableTime(candidate, interval))) {
                return candidate;
            }
            candidate = candidate.plus(step);
        }
        return null;
    }

    public boolean hasNext() { return nextTime != null; }

    public Instant next() {
        if (!hasNext()) throw new NoSuchElementException("No further historical cycle timestamps remain.");
        Instant current = nextTime;
        nextTime = nextExecutable(current.plus(step));
        return current;
    }

    public int getTotalCycles() { return totalCycles; }
    public Instant getEndTime() { return endTime; }
    public Instant getStartTime() { return startTime; }
}
