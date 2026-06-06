package com.algotrader.marketcalendar;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Market calendar for U.S. equity market trading hours.
 *
 * <p>This implementation determines market availability using a configured
 * market timezone, regular open and close times, full-market closure dates,
 * and early-close dates.
 *
 * <p>It supports the standard U.S. equities trading model:
 * <ul>
 *     <li>Trading occurs Monday through Friday</li>
 *     <li>Configured full-close dates are treated as non-trading days</li>
 *     <li>Configured early-close dates override the regular close time</li>
 * </ul>
 *
 * <p>This class is configuration-driven and is intended to be constructed
 * from market calendar configuration rather than hard-coded schedules.
 */
public final class UsMarketCalendar implements MarketCalendar {

    private final ZoneId marketZone;
    private final LocalTime regularOpen;
    private final LocalTime regularClose;
    private final Set<LocalDate> fullCloseDates;
    private final Map<LocalDate, LocalTime> earlyCloseDates;

    /**
     * Creates a U.S. market calendar.
     *
     * @param marketZone timezone used to interpret market dates and times
     * @param regularOpen regular market open time in local market time
     * @param regularClose regular market close time in local market time
     * @param fullCloseDates dates on which the market is completely closed
     * @param earlyCloseDates mapping of dates to special early-close times
     * @throws IllegalArgumentException if any argument is null
     */
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

    /**
     * Determines whether a timestamp falls within an open trading session.
     *
     * @param timestamp timestamp to evaluate
     * @return {@code true} if the market is open at the timestamp;
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
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

    /**
     * Returns the market open time for the market date containing the supplied
     * timestamp.
     *
     * @param timestamp timestamp whose market date should be examined
     * @return market open time for that market date
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
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

    /**
     * Returns the market close time for the market date containing the supplied
     * timestamp.
     *
     * <p>If the market date is configured as an early close, the early-close time
     * is returned instead of the regular close time.
     *
     * @param timestamp timestamp whose market date should be examined
     * @return market close time for that market date
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
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

    /**
     * Determines whether two timestamps belong to the same trading session.
     *
     * <p>For this implementation, timestamps are considered part of the same
     * trading session when they fall on the same market-local date and that date
     * is not a weekend or full-market closure.
     *
     * @param first first timestamp
     * @param second second timestamp
     * @return {@code true} if both timestamps belong to the same trading session
     * @throws IllegalArgumentException if either timestamp is null
     */
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

    /**
     * Determines whether a market date falls on a weekend.
     *
     * @param date market-local date to evaluate
     * @return {@code true} if the date is Saturday or Sunday
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }
}