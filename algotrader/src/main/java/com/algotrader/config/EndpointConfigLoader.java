package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link EndpointConfig} instances from JSON configuration files.
 *
 * <p>This loader resolves endpoint IDs to files in an endpoint configuration
 * directory. By default, configs are expected at:
 *
 * <pre>
 * config/endpoints/&lt;endpointId&gt;.json
 * </pre>
 *
 * <p>Each call to {@link #load(String)} reads and deserializes the matching
 * JSON file from disk. This class does not currently cache loaded configs.
 */
public class EndpointConfigLoader {

    private static final Path DEFAULT_ENDPOINT_CONFIG_DIRECTORY =
            Path.of("config", "endpoints");

    private final Path endpointConfigDirectory;
    private final ObjectMapper objectMapper;

    /**
     * Creates a loader that reads from the default endpoint config directory:
     *
     * <pre>
     * config/endpoints
     * </pre>
     */
    public EndpointConfigLoader() {
        this(DEFAULT_ENDPOINT_CONFIG_DIRECTORY);
    }

    /**
     * Creates a loader that reads endpoint configs from the given directory.
     *
     * @param endpointConfigDirectory the directory containing endpoint config JSON files
     * @throws IllegalArgumentException if {@code endpointConfigDirectory} is null
     */
    public EndpointConfigLoader(Path endpointConfigDirectory) {
        if (endpointConfigDirectory == null) {
            throw new IllegalArgumentException(
                    "Model config directory cannot be null."
            );
        }

        this.endpointConfigDirectory = endpointConfigDirectory;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads an endpoint configuration by endpoint ID.
     *
     * <p>For example, {@code load("cnn-v1")} reads:
     *
     * <pre>
     * config/endpoints/cnn-v1.json
     * </pre>
     *
     * @param endpointId the ID of the endpoint config to load, without the
     *        {@code .json} extension
     * @return the deserialized endpoint configuration
     * @throws IllegalArgumentException if {@code endpointId} is null or blank
     * @throws RuntimeException if the config file cannot be read or deserialized
     */
    public EndpointConfig load(String endpointId) {
        if (endpointId == null || endpointId.isBlank()) {
            throw new IllegalArgumentException(
                    "Model ID cannot be null or blank."
            );
        }

        Path configPath = endpointConfigDirectory.resolve(endpointId + ".json");

        try {
            return objectMapper.readValue(
                    configPath.toFile(),
                    EndpointConfig.class
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load model config from: " + configPath, //TODO: custom exception for config loaders
                    e
            );
        }
    }

    /**
     * Returns the directory this loader reads endpoint configuration files from.
     *
     * @return the endpoint config directory
     */
    public Path getEndpointConfigDirectory() {
        return endpointConfigDirectory;
    }

    // TODO: Build an EndpointConfigRegistry so configs can be loaded once
    //       and looked up by endpointId without repeatedly reading from disk.

    // TODO: Consider upgrading to DTO-based loading if JSON structure
    //       diverges from the immutable runtime EndpointConfig structure.
}
