package com.algotrader.clock;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.algotrader.marketcalendar.UsMarketCalendar;
import com.algotrader.marketdata.model.TimeInterval;

class HistoricalCycleClockTest {
    private final UsMarketCalendar calendar = new UsMarketCalendar(ZoneId.of("America/New_York"),
            LocalTime.of(9, 30), LocalTime.of(16, 0),
            Set.of(LocalDate.parse("2026-09-07"), LocalDate.parse("2026-11-26")),
            Map.of(LocalDate.parse("2026-11-27"), LocalTime.of(13, 0)));

    @Test void excludesNightsWeekendsAndHolidayAndIncludesFinalExecutableCandle() {
        var clock = clock("2026-09-04T19:50:00Z", "2026-09-08T13:40:00Z", TimeInterval.FIVE_MINUTES);
        assertEquals(5, clock.getTotalCycles());
        var times = drain(clock);
        assertEquals(List.of(Instant.parse("2026-09-04T19:50:00Z"), Instant.parse("2026-09-04T19:55:00Z"),
                Instant.parse("2026-09-08T13:30:00Z"), Instant.parse("2026-09-08T13:35:00Z"),
                Instant.parse("2026-09-08T13:40:00Z")), times);
        assertEquals(5, clock.getTotalCycles());
        assertThrows(NoSuchElementException.class, clock::next);
    }

    @Test void earlyCloseProducesOnlyFortyTwoFiveMinuteCycles() {
        var clock = clock("2026-11-26T00:00:00Z", "2026-11-28T00:00:00Z", TimeInterval.FIVE_MINUTES);
        assertEquals(42, clock.getTotalCycles());
        var times = drain(clock);
        assertEquals(Instant.parse("2026-11-27T14:30:00Z"), times.get(0));
        assertEquals(Instant.parse("2026-11-27T17:55:00Z"), times.get(times.size() - 1));
        assertTrue(times.stream().allMatch(calendar::isTradingTime));
    }

    @Test void oneMinuteClockHonorsExistingFiveMinuteExecutionBuffer() {
        var times = drain(clock("2026-11-27T17:54:00Z", "2026-11-27T18:00:00Z", TimeInterval.ONE_MINUTE));
        assertEquals(List.of(Instant.parse("2026-11-27T17:54:00Z"), Instant.parse("2026-11-27T17:55:00Z")), times);
    }

    @Test void calendarHandlesDaylightSavingChange() {
        var times = drain(clock("2026-03-06T20:55:00Z", "2026-03-09T13:35:00Z", TimeInterval.FIVE_MINUTES));
        assertEquals(List.of(Instant.parse("2026-03-06T20:55:00Z"), Instant.parse("2026-03-09T13:30:00Z"),
                Instant.parse("2026-03-09T13:35:00Z")), times);
    }

    @Test void closedOnlyRangeIsEmptyAndOffGridBoundsAreRejected() {
        var clock = clock("2026-09-05T13:30:00Z", "2026-09-06T19:55:00Z", TimeInterval.FIVE_MINUTES);
        assertEquals(0, clock.getTotalCycles());
        assertFalse(clock.hasNext());
        assertThrows(IllegalArgumentException.class,
                () -> clock("2026-09-08T13:31:00Z", "2026-09-08T14:00:00Z", TimeInterval.FIVE_MINUTES));
    }

    private HistoricalCycleClock clock(String start, String end, TimeInterval interval) {
        return new HistoricalCycleClock(Instant.parse(start), Instant.parse(end), interval, calendar);
    }

    private List<Instant> drain(HistoricalCycleClock clock) {
        List<Instant> times = new ArrayList<>();
        while (clock.hasNext()) times.add(clock.next());
        return times;
    }
}
