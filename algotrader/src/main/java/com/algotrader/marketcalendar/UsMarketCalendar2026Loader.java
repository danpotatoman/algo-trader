package com.algotrader.marketcalendar;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.algotrader.config.marketcalendar.UsMarketCalendarConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads the configured U.S. equity market calendar for 2026.
 *
 * <p>This loader reads a JSON calendar definition and constructs a
 * {@link UsMarketCalendar} containing market hours, holidays, and early-close
 * schedules.
 *
 * <p>The calendar is loaded from:
 *
 * <pre>
 * config/market-calendar/us-equities-2026.json
 * </pre>
 *
 * <p>This class currently targets a single calendar year and exists as a
 * simple configuration-loading mechanism for the market calendar subsystem.
 *
 * <p><b>TODO:</b> Consider replacing this loader with a more general
 * market-calendar loading system that supports multiple years and markets.
 */
public final class UsMarketCalendar2026Loader {

    private static final String DEFAULT_CALENDAR_PATH  =
            "config/market-calendar/us-equities-2026.json";

    private final ObjectMapper objectMapper;

    /**
     * Creates a market calendar loader.
     */
    public UsMarketCalendar2026Loader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads the configured U.S. equity market calendar.
     *
     * @return a fully configured market calendar
     * @throws RuntimeException if the calendar configuration cannot be loaded
     *         or parsed
     */
    public UsMarketCalendar load() {
        try {
            UsMarketCalendarConfig config =
                    objectMapper.readValue(
                            Path.of(DEFAULT_CALENDAR_PATH ).toFile(),
                            UsMarketCalendarConfig.class
                    );

            ZoneId marketZone =
                    ZoneId.of(config.timezone());

            LocalTime regularOpen =
                    LocalTime.parse(config.regularOpen());

            LocalTime regularClose =
                    LocalTime.parse(config.regularClose());

            Set<LocalDate> fullCloseDates =
                    config.fullCloseDates()
                            .stream()
                            .map(LocalDate::parse)
                            .collect(Collectors.toSet());

            Map<LocalDate, LocalTime> earlyCloseDates =
                    config.earlyCloseDates()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> LocalDate.parse(entry.getKey()),
                                    entry -> LocalTime.parse(entry.getValue())
                            ));

            return new UsMarketCalendar(
                    marketZone,
                    regularOpen,
                    regularClose,
                    fullCloseDates,
                    earlyCloseDates
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load market calendar from "
                            + DEFAULT_CALENDAR_PATH ,
                    e
            );
        }
    }
}