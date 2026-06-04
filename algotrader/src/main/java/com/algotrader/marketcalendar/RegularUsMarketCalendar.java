package com.algotrader.marketcalendar;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Market calendar for regular U.S. equity market hours.
 *
 * <p>This implementation assumes:
 * <ul>
 *     <li>Market timezone: America/New_York</li>
 *     <li>Regular open: 9:30 AM ET</li>
 *     <li>Regular close: 4:00 PM ET</li>
 *     <li>Monday through Friday trading</li>
 * </ul>
 *
 * <p>This implementation does not account for market holidays or early closes.
 */
public final class RegularUsMarketCalendar implements MarketCalendar {

    private static final ZoneId MARKET_ZONE =
            ZoneId.of("America/New_York");

    private static final LocalTime OPEN_TIME =
            LocalTime.of(9, 30);

    private static final LocalTime CLOSE_TIME =
            LocalTime.of(16, 0);

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

    private boolean isWeekend(
            LocalDate date
    ) {
        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }
}