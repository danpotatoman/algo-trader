package com.algotrader.marketcalendar;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import com.algotrader.marketdata.model.TimeInterval;

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
         * Returns the latest timestamp at which a trade can be executed on the
         * market-local date containing the supplied timestamp.
         *
         * <p>The returned time leaves enough time for the supplied interval to
         * complete before market close. Regardless of interval length, trading is
         * never permitted later than five minutes before market close.
         *
         * <p>For example, with a regular 4:00 PM close:
         * <ul>
         *     <li>A five-minute interval returns 3:55 PM</li>
         *     <li>A one-minute interval still returns 3:55 PM</li>
         *     <li>A fifteen-minute interval returns 3:45 PM</li>
         * </ul>
         *
         * <p>Configured early-close times are respected.
         *
         * @param timestamp timestamp whose market-local date should be examined
         * @param interval trading interval that must fit before market close
         * @return latest valid execution time for that market date
         * @throws IllegalArgumentException if either argument is null
         */
        @Override
        public Instant getLastExecutableTime(
                        Instant timestamp,
                        TimeInterval interval
                ) {
                if (timestamp == null) {
                        throw new IllegalArgumentException("Timestamp cannot be null.");
                }

                if (interval == null) {
                        throw new IllegalArgumentException("Interval cannot be null.");
                }

                Duration minimumCloseBuffer = Duration.ofMinutes(5);
                Duration intervalDuration = interval.getDuration();

                Duration closeBuffer =
                        intervalDuration.compareTo(minimumCloseBuffer) > 0
                                ? intervalDuration
                                : minimumCloseBuffer;

                return getMarketClose(timestamp).minus(closeBuffer);
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