package com.algotrader.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class TradingSessionLogWriter {

    private final ObjectMapper objectMapper;

    public TradingSessionLogWriter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(TradingSessionLog sessionLog, Path outputPath) {
        if (sessionLog == null) {
            throw new IllegalArgumentException("TradingSessionLog cannot be null.");
        }

        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null.");
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            objectMapper.writeValue(outputPath.toFile(), sessionLog);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write trading session log to: " + outputPath,
                    e
            );
        }
    }
}