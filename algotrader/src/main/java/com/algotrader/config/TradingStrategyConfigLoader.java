package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link TradingStrategyConfig} objects from JSON config files.
 *
 * <p>Strategy configs are expected to live at:
 *
 * <pre>
 * config/strategy/&lt;strategyId&gt;.json
 * </pre>
 */
public class TradingStrategyConfigLoader {

    private static final Path DEFAULT_STRATEGY_CONFIG_DIRECTORY =
            Path.of("config", "strategy");

    private final Path strategyConfigDirectory;
    private final ObjectMapper objectMapper;

    public TradingStrategyConfigLoader() {
        this(DEFAULT_STRATEGY_CONFIG_DIRECTORY);
    }

    public TradingStrategyConfigLoader(
            Path strategyConfigDirectory
    ) {
        if (strategyConfigDirectory == null) {
            throw new IllegalArgumentException(
                    "Strategy config directory cannot be null."
            );
        }

        this.strategyConfigDirectory = strategyConfigDirectory;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads a strategy config by strategy ID.
     *
     * <p>For example, {@code load("threshold-strategy-v1")} reads:
     *
     * <pre>
     * config/strategy/threshold-strategy-v1.json
     * </pre>
     *
     * @param strategyId the strategy ID to load
     * @return the loaded strategy config
     */
    public TradingStrategyConfig load(
            String strategyId
    ) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        Path configPath =
                strategyConfigDirectory.resolve(strategyId + ".json");

        try {
            return objectMapper.readValue(
                    configPath.toFile(),
                    TradingStrategyConfig.class
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load strategy config from: "
                            + configPath,
                    e
            );
        }
    }

    /**
     * Returns the directory this loader reads strategy configs from.
     *
     * @return the strategy config directory
     */
    public Path getStrategyConfigDirectory() {
        return strategyConfigDirectory;
    }

    // TODO: Build a TradingStrategyConfigRegistry so configs can be
    //       loaded once and reused without repeatedly reading from disk.

    // TODO: Consider upgrading to DTO-based loading if JSON structure
    //       diverges from immutable runtime TradingStrategyConfig objects.
}