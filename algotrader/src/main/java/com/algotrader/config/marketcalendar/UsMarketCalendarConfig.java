package com.algotrader.config.marketcalendar;

import java.util.List;
import java.util.Map;

public record UsMarketCalendarConfig(
        String market,
        String timezone,
        String regularOpen,
        String regularClose,
        List<String> fullCloseDates,
        Map<String, String> earlyCloseDates
) {
}