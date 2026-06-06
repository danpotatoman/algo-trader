package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link TradeGeneratorConfig} instances from JSON configuration files.
 *
 * <p>This loader resolves strategy IDs to files in a strategy configuration
 * directory. By default, configs are expected at:
 *
 * <pre>
 * config/strategy/&lt;strategyId&gt;.json
 * </pre>
 *
 * <p>Each call to {@link #load(String)} reads and deserializes the matching
 * JSON file from disk. This class does not currently cache loaded configs.
 */
public class TradeGeneratorConfigLoader {

    private static final Path DEFAULT_STRATEGY_CONFIG_DIRECTORY =
            Path.of("config", "strategy");

    private final Path strategyConfigDirectory;
    private final ObjectMapper objectMapper;

    /**
     * Creates a loader that reads from the default strategy config directory:
     *
     * <pre>
     * config/strategy
     * </pre>
     */
    public TradeGeneratorConfigLoader() {
        this(DEFAULT_STRATEGY_CONFIG_DIRECTORY);
    }

    /**
     * Creates a loader that reads strategy configs from the given directory.
     *
     * @param strategyConfigDirectory the directory containing strategy config
     *        JSON files
     * @throws IllegalArgumentException if {@code strategyConfigDirectory} is null
     */
    public TradeGeneratorConfigLoader(Path strategyConfigDirectory) {
        if (strategyConfigDirectory == null) {
            throw new IllegalArgumentException(
                    "Strategy config directory cannot be null."
            );
        }

        this.strategyConfigDirectory = strategyConfigDirectory;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads a trade generator configuration by strategy ID.
     *
     * <p>For example, {@code load("threshold-strategy-v1")} reads:
     *
     * <pre>
     * config/strategy/threshold-strategy-v1.json
     * </pre>
     *
     * @param strategyId the strategy ID to load, without the {@code .json}
     *        extension
     * @return the deserialized trade generator configuration
     * @throws IllegalArgumentException if {@code strategyId} is null or blank
     * @throws RuntimeException if the config file cannot be read or deserialized
     */
    public TradeGeneratorConfig load(String strategyId) {
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
                    TradeGeneratorConfig.class
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
     * Returns the directory this loader reads strategy configuration files from.
     *
     * @return the strategy config directory
     */
    public Path getStrategyConfigDirectory() {
        return strategyConfigDirectory;
    }

    // TODO: Build a TradeGeneratorConfigRegistry so configs can be
    //       loaded once and reused without repeatedly reading from disk.

    // TODO: Consider upgrading to DTO-based loading if JSON structure
    //       diverges from immutable runtime TradeGeneratorConfig objects.
}