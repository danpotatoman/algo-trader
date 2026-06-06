package com.algotrader.decision.generator.validation;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.marketcalendar.MarketCalendar;

/**
 * Validates whether a {@link RoundTripTrade} satisfies market calendar
 * constraints.
 *
 * <p>This validator ensures that a proposed trade can be executed within a
 * valid trading session and exited before the market closes. Trades that
 * violate market-hours rules or extend across multiple trading sessions are
 * considered invalid.
 *
 * <p>Validation rules currently include:
 * <ul>
 *     <li>The exit time must occur after the entry time</li>
 *     <li>The entry time must occur during market trading hours</li>
 *     <li>The exit time must occur during market trading hours</li>
 *     <li>The entry and exit must occur within the same trading session</li>
 *     <li>The exit must occur at least a configured duration before market
 *         close</li>
 * </ul>
 *
 * <p>This class is typically used by trade generation components to filter
 * out trades that cannot be safely executed according to the configured
 * market calendar.
 */
public final class RoundTripTradeValidator {

    private final MarketCalendar marketCalendar;
    private final Duration minTimeBeforeClose;

    /**
     * Creates a round-trip trade validator.
     *
     * @param marketCalendar market calendar used to determine trading sessions
     *        and market hours
     * @param minTimeBeforeClose minimum amount of time that must remain before
     *        market close when a trade exits
     * @throws IllegalArgumentException if any argument is invalid
     */
    public RoundTripTradeValidator(
            MarketCalendar marketCalendar,
            Duration minTimeBeforeClose
    ) {
        if (marketCalendar == null) {
            throw new IllegalArgumentException(
                    "MarketCalendar cannot be null."
            );
        }

        if (minTimeBeforeClose == null || minTimeBeforeClose.isNegative()) {
            throw new IllegalArgumentException(
                    "Minimum time before close cannot be null or negative."
            );
        }

        this.marketCalendar = marketCalendar;
        this.minTimeBeforeClose = minTimeBeforeClose;
    }

    /**
     * Determines whether a round-trip trade satisfies all configured market
     * calendar constraints.
     *
     * @param trade trade to validate
     * @return {@code true} if the trade is valid; {@code false} otherwise
     * @throws IllegalArgumentException if {@code trade} is null
     */
    public boolean isValid(
            RoundTripTrade trade
    ) {
        if (trade == null) {
            throw new IllegalArgumentException(
                    "RoundTripTrade cannot be null."
            );
        }

        Instant entryTime = trade.entryTime();
        Instant exitTime = trade.exitTime();

        if (!exitTime.isAfter(entryTime)) {
            return false;
        }

        if (!marketCalendar.isTradingTime(entryTime)) {
            return false;
        }

        if (!marketCalendar.isTradingTime(exitTime)) {
            return false;
        }

        if (!marketCalendar.isSameTradingSession(entryTime, exitTime)) {
            return false;
        }

        Instant marketClose =
                marketCalendar.getMarketClose(exitTime);

        Instant latestAllowedExit =
                marketClose.minus(minTimeBeforeClose);

        return !exitTime.isAfter(latestAllowedExit);
    }
}