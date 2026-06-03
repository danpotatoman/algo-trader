package com.algotrader.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link ModelConfig} objects from JSON config files.
 *
 * <p>Model configs are expected to live at:
 *
 * <pre>
 * config/models/&lt;modelId&gt;.json
 * </pre>
 */
public class ModelConfigLoader {

    private static final Path DEFAULT_MODEL_CONFIG_DIRECTORY =
            Path.of("config", "models");

    private final Path modelConfigDirectory;
    private final ObjectMapper objectMapper;

    public ModelConfigLoader() {
        this(DEFAULT_MODEL_CONFIG_DIRECTORY);
    }

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
     * Loads a model config by model ID.
     *
     * <p>For example, {@code load("cnn-v1")} reads:
     *
     * <pre>
     * config/models/cnn-v1.json
     * </pre>
     *
     * @param modelId the model ID to load
     * @return the loaded model config
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
     * Returns the directory this loader reads model configs from.
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