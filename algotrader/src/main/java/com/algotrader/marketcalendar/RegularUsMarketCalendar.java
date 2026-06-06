package com.algotrader.marketcalendar;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Simple market calendar for regular U.S. equity trading hours.
 *
 * <p>This implementation models the standard U.S. equity trading session:
 * <ul>
 *     <li>Timezone: America/New_York</li>
 *     <li>Market open: 09:30 ET</li>
 *     <li>Market close: 16:00 ET</li>
 *     <li>Trading days: Monday through Friday</li>
 * </ul>
 *
 * <p>This calendar intentionally ignores exchange holidays, special market
 * closures, and early-close trading sessions. It should therefore be viewed
 * as an approximation of market availability rather than a complete market
 * schedule.
 *
 * <p>This implementation was originally used during early development of the
 * trading system before support for configuration-driven market calendars was
 * introduced.
 */
public final class RegularUsMarketCalendar implements MarketCalendar {

    private static final ZoneId MARKET_ZONE =
            ZoneId.of("America/New_York");

    private static final LocalTime OPEN_TIME =
            LocalTime.of(9, 30);

    private static final LocalTime CLOSE_TIME =
            LocalTime.of(16, 0);


    /**
     * Determines whether a timestamp falls within regular trading hours.
     *
     * @param timestamp timestamp to evaluate
     * @return {@code true} if the timestamp falls on a weekday between
     *         market open and market close; {@code false} otherwise
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
    @Override
    public boolean isTradingTime(
            Instant timestamp
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
        }

        ZonedDateTime marketDateTime =
                timestamp.atZone(MARKET_ZONE);

        if (isWeekend(marketDateTime.toLocalDate())) {
            return false;
        }

        LocalTime localTime = marketDateTime.toLocalTime();

        return !localTime.isBefore(OPEN_TIME)
                && localTime.isBefore(CLOSE_TIME);
    }

    /**
     * Returns the market open time for the trading day associated with the
     * supplied timestamp.
     *
     * @param timestamp timestamp whose trading day should be examined
     * @return market open time for that day
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
    @Override
    public Instant getMarketOpen(
            Instant timestamp
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
        }

        LocalDate marketDate =
                timestamp.atZone(MARKET_ZONE).toLocalDate();

        return marketDate
                .atTime(OPEN_TIME)
                .atZone(MARKET_ZONE)
                .toInstant();
    }

    /**
     * Returns the market close time for the trading day associated with the
     * supplied timestamp.
     *
     * @param timestamp timestamp whose trading day should be examined
     * @return market close time for that day
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
    @Override
    public Instant getMarketClose(
            Instant timestamp
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
        }

        LocalDate marketDate =
                timestamp.atZone(MARKET_ZONE).toLocalDate();

        return marketDate
                .atTime(CLOSE_TIME)
                .atZone(MARKET_ZONE)
                .toInstant();
    }

    /**
     * Determines whether two timestamps belong to the same trading session.
     *
     * <p>For this implementation, timestamps are considered part of the same
     * session when they occur on the same non-weekend calendar date in the
     * market timezone.
     *
     * @param first first timestamp
     * @param second second timestamp
     * @return {@code true} if both timestamps belong to the same trading
     *         session; {@code false} otherwise
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

        ZonedDateTime firstMarketDateTime =
                first.atZone(MARKET_ZONE);

        ZonedDateTime secondMarketDateTime =
                second.atZone(MARKET_ZONE);

        LocalDate firstDate =
                firstMarketDateTime.toLocalDate();

        LocalDate secondDate =
                secondMarketDateTime.toLocalDate();

        return firstDate.equals(secondDate)
                && !isWeekend(firstDate)
                && !isWeekend(secondDate);
    }

    /**
     * Determines whether a market date falls on a weekend.
     *
     * @param date market date to evaluate
     * @return {@code true} if the date is Saturday or Sunday
     */
    private boolean isWeekend(
            LocalDate date
    ) {
        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }
}