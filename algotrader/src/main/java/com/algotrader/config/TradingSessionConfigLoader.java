package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Loads {@link TradingSessionConfig} instances from JSON configuration files.
 *
 * <p>This loader resolves session IDs to files in the default session
 * configuration directory:
 *
 * <pre>
 * config/session/&lt;sessionId&gt;.json
 * </pre>
 *
 * <p>The loader registers Jackson's {@link JavaTimeModule} so that Java time
 * types such as {@link java.time.Instant} and {@link java.time.Duration} can
 * be deserialized from JSON.
 *
 * <p>Each call to {@link #load(String)} reads and deserializes the matching
 * JSON file from disk. This class does not currently cache loaded configs.
 */
public final class TradingSessionConfigLoader {

    private static final Path DEFAULT_SESSION_CONFIG_DIRECTORY = Path.of("config", "session");

    private final ObjectMapper objectMapper;
    /**
     * Creates a loader that reads trading session configs from the default
     * session config directory:
     *
     * <pre>
     * config/session
     * </pre>
     */
    public TradingSessionConfigLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Loads a trading session configuration by session ID.
     *
     * <p>For example, {@code load("aapl-cnn-v1-5m")} reads:
     *
     * <pre>
     * config/session/aapl-cnn-v1-5m.json
     * </pre>
     *
     * @param sessionId the session ID to load, without the {@code .json}
     *        extension
     * @return the deserialized trading session configuration
     * @throws IllegalArgumentException if {@code sessionId} is null or blank
     * @throws RuntimeException if the config file cannot be read or deserialized
     */
    public TradingSessionConfig load(String sessionId) {
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