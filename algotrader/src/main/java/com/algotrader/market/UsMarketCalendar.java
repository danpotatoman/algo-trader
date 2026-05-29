package com.algotrader.market;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Market calendar for U.S. equity market hours.
 *
 * <p>This implementation assumes:
 * <ul>
 *     <li>Market timezone: America/New_York</li>
 *     <li>Regular open: 9:30 AM ET</li>
 *     <li>Regular close: 4:00 PM ET</li>
 *     <li>Monday through Friday trading</li>
 * </ul>
 *
 * <p>This implementation accounts for market holidays and early closes.
 */
public final class UsMarketCalendar implements MarketCalendar {

    private final ZoneId marketZone;
    private final LocalTime regularOpen;
    private final LocalTime regularClose;
    private final Set<LocalDate> fullCloseDates;
    private final Map<LocalDate, LocalTime> earlyCloseDates;

    public UsMarketCalendar(
            ZoneId marketZone,
            LocalTime regularOpen,
            LocalTime regularClose,
            Set<LocalDate> fullCloseDates,
            Map<LocalDate, LocalTime> earlyCloseDates
    ) {
        if (marketZone == null
                || regularOpen == null
                || regularClose == null
                || fullCloseDates == null
                || earlyCloseDates == null) {
            throw new IllegalArgumentException(
                    "Market calendar fields cannot be null."
            );
        }

        this.marketZone = marketZone;
        this.regularOpen = regularOpen;
        this.regularClose = regularClose;
        this.fullCloseDates = Set.copyOf(fullCloseDates);
        this.earlyCloseDates = Map.copyOf(earlyCloseDates);
    }

    @Override
    public boolean isTradingTime(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        ZonedDateTime marketDateTime = timestamp.atZone(marketZone);
        LocalDate date = marketDateTime.toLocalDate();
        LocalTime time = marketDateTime.toLocalTime();

        if (isWeekend(date) || fullCloseDates.contains(date)) {
            return false;
        }

        LocalTime closeTime = earlyCloseDates.getOrDefault(
                date,
                regularClose
        );

        return !time.isBefore(regularOpen)
                && time.isBefore(closeTime);
    }

    @Override
    public Instant getMarketOpen(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        LocalDate date = timestamp.atZone(marketZone).toLocalDate();

        return date
                .atTime(regularOpen)
                .atZone(marketZone)
                .toInstant();
    }

    @Override
    public Instant getMarketClose(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        LocalDate date = timestamp.atZone(marketZone).toLocalDate();

        LocalTime closeTime = earlyCloseDates.getOrDefault(
                date,
                regularClose
        );

        return date
                .atTime(closeTime)
                .atZone(marketZone)
                .toInstant();
    }

    @Override
    public boolean isSameTradingSession(
            Instant first,
            Instant second
    ) {
        if (first == null || second == null) {
            throw new IllegalArgumentException(
                    "Timestamps cannot be null."
            );
        }

        LocalDate firstDate =
                first.atZone(marketZone).toLocalDate();

        LocalDate secondDate =
                second.atZone(marketZone).toLocalDate();

        return firstDate.equals(secondDate)
                && !isWeekend(firstDate)
                && !fullCloseDates.contains(firstDate);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }
}