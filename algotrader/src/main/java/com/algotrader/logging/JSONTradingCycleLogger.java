package com.algotrader.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * {@link TradingCycleLogger} implementation that persists trading cycle logs
 * as JSON files.
 *
 * <p>This implementation stores each trading cycle in a separate file rather
 * than appending to a shared log, making individual cycles easy to inspect and
 * process independently.
 * 
 * <p>Each {@link TradingCycleLog} is serialized into a standalone,
 * human-readable JSON document stored on disk. These logs can be used for
 * debugging, auditing, performance analysis, and backtesting analytics.
 *
 * <p>By default, logs are written to:
 *
 * <pre>
 * data/logs/trading-cycles
 * </pre>
 *
 * <p>The logger automatically creates missing directories and configures
 * Jackson to produce indented JSON with ISO-8601 date/time formatting.
 */
public final class JSONTradingCycleLogger
        implements TradingCycleLogger {

    private static final Path DEFAULT_LOG_DIRECTORY =
            Path.of("data", "logs", "trading-cycles");

    private final Path logDirectory;
    private final ObjectMapper objectMapper;

    /**
     * Creates a logger that writes trading cycle logs to the default log
     * directory.
     */
    public JSONTradingCycleLogger() {
        this(DEFAULT_LOG_DIRECTORY);
    }

    /**
     * Creates a logger that writes trading cycle logs to the supplied directory.
     *
     * @param logDirectory directory where JSON log files will be written
     * @throws IllegalArgumentException if {@code logDirectory} is null
     */
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

    /**
     * Writes a trading cycle log to disk as a JSON document.
     *
     * <p>The output filename is derived from the cycle ID and sanitized to
     * remove characters that are invalid in common filesystems.
     *
     * @param log trading cycle log to persist
     * @throws IllegalArgumentException if {@code log} is null
     * @throws RuntimeException if the log cannot be written
     */
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

    /**
     * Produces a filesystem-safe filename from a cycle identifier.
     *
     * @param fileName original filename or identifier
     * @return sanitized filename suitable for use on common filesystems
     */
    private String sanitizeFileName(String fileName) {
        return fileName
                .replace(":", "-")
                .replace("/", "-")
                .replace("\\", "-");
    }
}