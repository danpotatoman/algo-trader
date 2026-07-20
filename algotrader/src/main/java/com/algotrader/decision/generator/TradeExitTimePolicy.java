package com.algotrader.decision.generator;

import com.algotrader.marketcalendar.MarketCalendar;
import com.algotrader.marketdata.model.TimeInterval;

import java.time.Instant;

/**
 * Determines the latest valid exit time for generated and open trades.
 *
 * <p>The policy combines market-calendar execution boundaries with the
 * configured session end time so planned exits do not extend past either
 * constraint.
 */
public final class TradeExitTimePolicy {

    private final MarketCalendar marketCalendar;
    private final TimeInterval interval;
    private final Instant sessionEndTime;

    /**
     * Creates a trade exit-time policy.
     *
     * @param marketCalendar calendar used to determine market execution
     *        boundaries
     * @param interval market data interval used to leave room before market
     *        close
     * @param sessionEndTime final timestamp allowed by the trading session
     * @throws IllegalArgumentException if any argument is null
     */
    public TradeExitTimePolicy(MarketCalendar marketCalendar, TimeInterval interval, Instant sessionEndTime) {
        if (marketCalendar == null) {
            throw new IllegalArgumentException("marketCalendar cannot be null.");
        }
        if (interval == null) {
            throw new IllegalArgumentException("interval cannot be null.");
        }
        if (sessionEndTime == null) {
            throw new IllegalArgumentException("sessionEndTime cannot be null.");
        }

        this.marketCalendar = marketCalendar;
        this.interval = interval;
        this.sessionEndTime = sessionEndTime;
    }

    /**
     * Returns the latest exit time allowed for the reference timestamp.
     *
     * @param referenceTime timestamp whose market session should be examined
     * @return earliest applicable boundary from the market calendar and session
     *         end time
     */
    public Instant latestAllowedExit(Instant referenceTime) {
        Instant marketBoundary =
                marketCalendar.getLastExecutableTime(
                        referenceTime,
                        interval
                );

        return marketBoundary.isBefore(sessionEndTime)
                ? marketBoundary
                : sessionEndTime;
    }

    /**
     * Returns whether the proposed exit time satisfies this policy.
     *
     * @param referenceTime timestamp whose market session should be examined
     * @param exitTime proposed exit time
     * @return {@code true} if the exit is not later than the latest allowed
     *         exit time
     */
    public boolean isValidExitTime(
            Instant referenceTime,
            Instant exitTime
    ) {
        return !exitTime.isAfter(latestAllowedExit(referenceTime));
    }
}
