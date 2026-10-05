package com.algotrader.clock;

import com.algotrader.runtime.ResolvedTradingPlan;
import com.algotrader.marketcalendar.MarketCalendar;

/**
 * Factory for creating {@link HistoricalCycleClock} instances.
 */
public final class HistoricalCycleClockFactory {

    /**
     * Creates a historical cycle clock for the supplied trading plan.
     *
     * @param tradingPlan resolved trading plan
     * @param calendar market calendar shared with execution policies
     * @return initialized historical cycle clock
     * @throws IllegalArgumentException if {@code tradingPlan} is {@code null}
     */
    public HistoricalCycleClock create(
            ResolvedTradingPlan tradingPlan,
            MarketCalendar calendar
    ) {
        if (tradingPlan == null) {
            throw new IllegalArgumentException(
                    "Resolved trading plan cannot be null."
            );
        }

        return new HistoricalCycleClock(
                tradingPlan.getFirstCandleTimestamp(),
                tradingPlan.getLastCandleTimestamp(),
                tradingPlan.getInterval(),
                calendar
        );
    }
}
