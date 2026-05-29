package com.algotrader.strategy.validation;

import java.time.Duration;
import java.time.Instant;

import com.algotrader.market.MarketCalendar;
import com.algotrader.strategy.RoundTripTrade;

public final class RoundTripTradeValidator {

    private final MarketCalendar marketCalendar;
    private final Duration minTimeBeforeClose;

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