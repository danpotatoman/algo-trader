package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Loads {@link TradingSessionConfig} instances from JSON files.
 */
public final class TradingSessionConfigLoader {

    private static final Path DEFAULT_SESSION_CONFIG_DIRECTORY =
            Path.of("config", "session");

    private final ObjectMapper objectMapper;

    public TradingSessionConfigLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Loads a trading session configuration by ID.
     *
     * <p>The configuration is expected at:
     *
     * <pre>
     * config/session/{sessionId}.json
     * </pre>
     *
     * @param sessionId unique session identifier
     * @return loaded session configuration
     * @throws IllegalArgumentException if the session ID is invalid
     * @throws RuntimeException if the configuration cannot be loaded
     */
    public TradingSessionConfig load(
            String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Session ID cannot be null or blank."
            );
        }

        Path configPath = DEFAULT_SESSION_CONFIG_DIRECTORY.resolve(
                sessionId + ".json"
        );

        try {
            return objectMapper.readValue(
                    Files.newBufferedReader(configPath),
                    TradingSessionConfig.class
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load trading session config from: "
                            + configPath,
                    e
            );
        }
    }
}