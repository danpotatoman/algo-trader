package com.algotrader.market;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class UsMarketCalendar2026Loader {

    private static final String RESOURCE_PATH =
            "config/market-calendar/us-equities-2026.json";

    private final ObjectMapper objectMapper;

    public UsMarketCalendar2026Loader() {
        this.objectMapper = new ObjectMapper();
    }

    public UsMarketCalendar load() {
        try {
            UsMarketCalendarConfig config =
                    objectMapper.readValue(
                            Path.of(RESOURCE_PATH).toFile(),
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
                            + RESOURCE_PATH,
                    e
            );
        }
    }
}