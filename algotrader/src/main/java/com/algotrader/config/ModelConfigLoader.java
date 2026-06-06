package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link ModelConfig} instances from JSON configuration files.
 *
 * <p>This loader resolves model IDs to files in a model configuration
 * directory. By default, configs are expected at:
 *
 * <pre>
 * config/models/&lt;modelId&gt;.json
 * </pre>
 *
 * <p>Each call to {@link #load(String)} reads and deserializes the matching
 * JSON file from disk. This class does not currently cache loaded configs.
 */
public class ModelConfigLoader {

    private static final Path DEFAULT_MODEL_CONFIG_DIRECTORY =
            Path.of("config", "models");

    private final Path modelConfigDirectory;
    private final ObjectMapper objectMapper;

    /**
     * Creates a loader that reads from the default model config directory:
     *
     * <pre>
     * config/models
     * </pre>
     */
    public ModelConfigLoader() {
        this(DEFAULT_MODEL_CONFIG_DIRECTORY);
    }

    /**
     * Creates a loader that reads model configs from the given directory.
     *
     * @param modelConfigDirectory the directory containing model config JSON files
     * @throws IllegalArgumentException if {@code modelConfigDirectory} is null
     */
    public ModelConfigLoader(Path modelConfigDirectory) {
        if (modelConfigDirectory == null) {
            throw new IllegalArgumentException(
                    "Model config directory cannot be null."
            );
        }

        this.modelConfigDirectory = modelConfigDirectory;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads a model configuration by model ID.
     *
     * <p>For example, {@code load("cnn-v1")} reads:
     *
     * <pre>
     * config/models/cnn-v1.json
     * </pre>
     *
     * @param modelId the ID of the model config to load, without the
     *        {@code .json} extension
     * @return the deserialized model configuration
     * @throws IllegalArgumentException if {@code modelId} is null or blank
     * @throws RuntimeException if the config file cannot be read or deserialized
     */
    public ModelConfig load(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "Model ID cannot be null or blank."
            );
        }

        Path configPath = modelConfigDirectory.resolve(modelId + ".json");

        try {
            return objectMapper.readValue(
                    configPath.toFile(),
                    ModelConfig.class
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load model config from: " + configPath, //TODO: custom exception for config loaders
                    e
            );
        }
    }

    /**
     * Returns the directory this loader reads model configuration files from.
     *
     * @return the model config directory
     */
    public Path getModelConfigDirectory() {
        return modelConfigDirectory;
    }

    // TODO: Build a ModelConfigRegistry so configs can be loaded once
    //       and looked up by modelId without repeatedly reading from disk.

    // TODO: Consider upgrading to DTO-based loading if JSON structure
    //       diverges from the immutable runtime ModelConfig structure.
}