package com.algotrader.data.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSONTradingCycleLogger implements TradingCycleLogger {

    private static final Path DEFAULT_LOG_DIRECTORY =
            Path.of("data", "logs", "trading-cycles");

    private final Path logDirectory;
    private final ObjectMapper objectMapper;

    public JSONTradingCycleLogger() {
        this(DEFAULT_LOG_DIRECTORY);
    }

    public JSONTradingCycleLogger(Path logDirectory) {
        if (logDirectory == null) {
            throw new IllegalArgumentException(
                    "Log directory cannot be null."
            );
        }

        this.logDirectory = logDirectory;
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void log(TradingCycleLog log) {
        if (log == null) {
            throw new IllegalArgumentException(
                    "TradingCycleLog cannot be null."
            );
        }

        try {
            Files.createDirectories(logDirectory);

            String fileName = sanitizeFileName(log.getCycleId()) + ".json";

            Path outputPath = logDirectory.resolve(fileName);

            objectMapper.writeValue(
                    outputPath.toFile(),
                    log
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write trading cycle log: "
                            + log.getCycleId(),
                    e
            );
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName
                .replace(":", "-")
                .replace("/", "-")
                .replace("\\", "-");
    }
}