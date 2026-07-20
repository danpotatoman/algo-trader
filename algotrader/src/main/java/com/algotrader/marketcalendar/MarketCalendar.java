package com.algotrader.marketcalendar;

import java.time.Instant;

import com.algotrader.marketdata.model.TimeInterval;

/**
 * Provides information about market trading sessions.
 *
 * <p>Implementations determine whether a timestamp falls within market
 * trading hours and provide session boundary information such as market
 * open and close times.
 *
 * <p>Examples of implementations include:
 * <ul>
 *     <li>Regular U.S. equity market hours</li>
 *     <li>Markets with holiday calendars</li>
 *     <li>Markets with early-close schedules</li>
 *     <li>24/7 markets such as cryptocurrencies</li>
 * </ul>
 */
public interface MarketCalendar {

        /**
         * Returns whether the specified timestamp falls within an active
         * trading session.
         *
         * @param timestamp the timestamp to evaluate
         * @return true if trading is permitted at the specified time
         * @throws IllegalArgumentException if timestamp is null
         */
        boolean isTradingTime(
                Instant timestamp
        );

        /**
         * Returns the market open time for the trading session containing
         * the specified timestamp.
         *
         * @param timestamp a timestamp within or near the session
         * @return the session open time
         * @throws IllegalArgumentException if timestamp is null
         */
        Instant getMarketOpen(
                Instant timestamp
        );

        /**
         * Returns the market close time for the trading session containing
         * the specified timestamp.
         *
         * @param timestamp a timestamp within or near the session
         * @return the session close time
         * @throws IllegalArgumentException if timestamp is null
         */
        Instant getMarketClose(
                Instant timestamp
        );

        /**
         * Returns whether both timestamps belong to the same trading session.
         *
         * @param first the first timestamp
         * @param second the second timestamp
         * @return true if both timestamps occur during the same session
         * @throws IllegalArgumentException if either timestamp is null
         */
        boolean isSameTradingSession(
                Instant first,
                Instant second
        );

        /**
         * Returns the latest valid execution time on the market-local date containing
         * the supplied timestamp.
         *
         * @param timestamp timestamp whose trading date should be examined
         * @param interval trading interval that must fit before market close
         * @return latest valid execution time
         * @throws IllegalArgumentException if either argument is null
         */
        Instant getLastExecutableTime(
                Instant timestamp,
                TimeInterval interval
        );

        /**
         * Returns whether an entry and exit time form a valid round-trip
         * trade according to this market calendar.
         *
         * <p>The default implementation requires:
         * <ul>
         *     <li>Both timestamps to be trading times</li>
         *     <li>The exit to occur after the entry</li>
         *     <li>Both timestamps to belong to the same trading session</li>
         * </ul>
         *
         * @param entryTime the proposed entry time
         * @param exitTime the proposed exit time
         * @return true if the round trip is valid
         */
        default boolean isValidRoundTrip(
                Instant entryTime,
                Instant exitTime
        ) {
                if (entryTime == null || exitTime == null) {
                throw new IllegalArgumentException(
                        "Entry and exit times cannot be null."
                );
                }

                return exitTime.isAfter(entryTime)
                        && isTradingTime(entryTime)
                        && isTradingTime(exitTime)
                        && isSameTradingSession(entryTime, exitTime);
        }
}
