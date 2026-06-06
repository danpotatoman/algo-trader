package com.algotrader.config.marketcalendar;

import java.util.List;
import java.util.Map;

/**
 * Configuration describing the operating schedule of a financial market.
 *
 * <p>This record defines the market's timezone, standard trading hours,
 * full-market holidays, and special early-close sessions. It is typically
 * loaded from a JSON configuration file and used by market calendar services
 * to determine whether trading is permitted at a given time.
 *
 * <p>Times are represented as local market times using the {@code HH:mm}
 * format. Dates are represented using the ISO-8601 {@code yyyy-MM-dd}
 * format.
 *
 * @param market the market identifier (e.g. {@code "US_EQUITIES"})
 * @param timezone the IANA timezone identifier used by the market
 *        (e.g. {@code "America/New_York"})
 * @param regularOpen the standard daily market open time in {@code HH:mm}
 *        format (e.g. {@code "09:30"})
 * @param regularClose the standard daily market close time in {@code HH:mm}
 *        format (e.g. {@code "16:00"})
 * @param fullCloseDates dates on which the market is completely closed,
 *        represented as ISO-8601 {@code yyyy-MM-dd} strings
 * @param earlyCloseDates mapping of ISO-8601 dates to special market close
 *        times in {@code HH:mm} format for shortened trading sessions
 */
public record UsMarketCalendarConfig(
        String market,
        String timezone,
        String regularOpen,
        String regularClose,
        List<String> fullCloseDates,
        Map<String, String> earlyCloseDates
) {
}