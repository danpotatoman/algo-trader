package com.algotrader.logging;

import com.algotrader.runtime.ResolvedTradingPlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Owns a run's immutable inputs, manifest, and completion marker. No environment or credential dumps. */
public final class RunArtifacts {
    public static final String SCHEMA = "algotrader-run-v1";
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path directory;
    private final String runId;
    private final long startNanos = System.nanoTime();
    private final Instant startedAt = Instant.now();
    private final Map<String, Object> manifest = new LinkedHashMap<>();

    public RunArtifacts(Path outputRoot) throws IOException {
        runId = UUID.randomUUID().toString();
        directory = Files.createDirectories(outputRoot.resolve(runId));
        manifest.put("schemaVersion", SCHEMA);
        manifest.put("runId", runId);
        manifest.put("startedAt", startedAt);
        write("manifest.json", manifest);
    }

    public Path directory() { return directory; }
    public String runId() { return runId; }
    public Path database() { return directory.resolve("ohlcv.db"); }

    /** SQLite creates a consistent snapshot, including committed WAL contents. The run reads this copy. */
    public void prepare(Path projectRoot, ResolvedTradingPlan plan) throws Exception {
        Path source = projectRoot.resolve("data/ohlcv.db").toAbsolutePath();
        if (!Files.isRegularFile(source)) throw new IOException("OHLCV database does not exist");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + source);
             var statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + database().toAbsolutePath().toString().replace("'", "''") + "'");
        }
        manifest.put("data", Map.of("source", "data/ohlcv.db", "snapshot", "ohlcv.db",
                "sha256", sha256(database()), "bytes", Files.size(database()),
                "snapshotCreatedAt", Instant.now(), "policy", "Run reads only this SQLite snapshot"));
        var session = plan.getSessionConfig();
        manifest.put("session", Map.of("sessionId", plan.getSessionId(), "endpointId", plan.getEndpointId(),
                "strategyId", plan.getStrategyId(), "tickers", plan.getTickers(),
                "historicalStart", plan.getFirstCandleTimestamp(), "historicalEnd", plan.getLastCandleTimestamp(),
                "startingCash", session.getStartingCash()));
        var endpoint = plan.getEndpointConfig();
        // Deliberately omit user-info, query, fragment, and free-text descriptions.
        var uri = endpoint.getEndpoint();
        String safeEndpoint = new java.net.URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                uri.getPath(), null, null).toString();
        manifest.put("endpoint", Map.of("endpointId", endpoint.getEndpointId(), "endpoint", safeEndpoint,
                "predictionType", endpoint.getPredictionType(), "interval", endpoint.getInterval(),
                "numCandles", endpoint.getNumCandles(), "version", endpoint.getVersion(),
                "outputStatistics", endpoint.getOutputStatistics()));
        var strategy = plan.getTradeGeneratorConfig();
        Map<String, Object> parameters = new TreeMap<>();
        // Only the current strategy's numeric settings are exportable.
        for (String key : List.of("minConfidenceThreshold", "minPositionFraction", "maxPositionFraction",
                "cashAllocationFractionPerCycle", "maxAllocationFractionPerTickerPerCycle")) {
            if (strategy.getParameters().has(key)) parameters.put(key, strategy.getParameters().getRequiredDouble(key));
        }
        manifest.put("strategy", Map.of("strategyId", strategy.getStrategyId(), "strategyType", strategy.getStrategyType(),
                "version", strategy.getVersion(), "parameters", parameters, "holdingPeriodMinutes", 30));
        Path calendar = projectRoot.resolve("config/market-calendar/us-equities-2026.json");
        if (Files.isRegularFile(calendar)) {
            // Deserialize the typed calendar rather than copying arbitrary JSON fields.
            manifest.put("marketCalendar", JSON.readValue(calendar.toFile(),
                    com.algotrader.config.marketcalendar.UsMarketCalendarConfig.class));
        }
        String status = git(projectRoot, "status", "--porcelain");
        manifest.put("git", Map.of("commit", git(projectRoot, "rev-parse", "HEAD").trim(),
                "dirty", status.equals("UNAVAILABLE") ? "UNKNOWN" : !status.isBlank(),
                "sourceFileSha256", sourceHashes(projectRoot)));
        Path artifact = Path.of(RunArtifacts.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        manifest.put("javaBuild", Map.of("kind", Files.isRegularFile(artifact) ? "jar" : "classes",
                "sha256", Files.isRegularFile(artifact) ? sha256(artifact) : hashClasses(artifact)));
        manifest.put("runtime", Map.of("javaVersion", System.getProperty("java.version"),
                "os", System.getProperty("os.name"), "architecture", System.getProperty("os.arch"),
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "maxHeapBytes", Runtime.getRuntime().maxMemory(),
                "cpuDescription", Objects.toString(System.getenv("PROCESSOR_IDENTIFIER"), "UNAVAILABLE")));
        manifest.put("assumptions", List.of(
                "Long-only fractional shares; no fees, spread, slippage, partial fills, or execution delay",
                "Fills use an exact timestamp's candle open from MarketDataCache's supported intervals",
                "Predictions use completed candles only; window timestamps denote candle opens",
                "POST_CYCLE marks follow exits, evaluation, entries and exit adjustments at the cycle timestamp",
                "Missing marks carry the last observed opening/fill price, explicitly STALE; unknown equity is null",
                "Final liquidation is attempted at the final visited cycle time, never at an earlier timestamp",
                "Clock follows the configured market calendar and last-executable-time policy, including early closes",
                "Preflight requires every execution candle and complete model windows; later session mornings may be warm-up only",
                "Invalid windows are omitted (missing, insufficient, or non-contiguous candles)",
                "Model-service warm-up is not removed from timings; first inference is identifiable; no strategy optimization performed"));
        manifest.put("dataLineage", "SQLite is authoritative. The ingestion script uses yfinance auto_adjust=False; per-row vendor/download lineage is not stored and cannot be certified retrospectively.");
        write("manifest.json", manifest);
    }

    public void modelProvenance(JsonNode provenance) throws IOException {
        manifest.put("predictionService", provenance);
        write("manifest.json", manifest);
    }

    public void historicalCoverage(com.algotrader.runtime.HistoricalDataCoverageValidator.Coverage coverage) throws IOException {
        manifest.put("historicalCoverage", coverage);
        write("manifest.json", manifest);
    }

    public void complete(TradingSessionLog session, long driverNanos, long sessionWriteNanos) throws IOException {
        Map<String, Object> completion = new LinkedHashMap<>();
        completion.put("schemaVersion", SCHEMA);
        completion.put("runId", runId);
        completion.put("status", session.sessionFailure() != null ? "FAILED"
                : session.finalPortfolio().positions().isEmpty() ? "COMPLETED" : "COMPLETED_WITH_OPEN_POSITIONS");
        completion.put("finishedAt", Instant.now());
        completion.put("fullBacktestWallNanos", System.nanoTime() - startNanos);
        completion.put("driverNanos", driverNanos);
        completion.put("sessionWriteNanos", sessionWriteNanos);
        completion.put("timingScope", "Full wall time includes provenance, snapshot, initialization, cycles, liquidation and session writing; excludes JVM startup and this completion marker");
        completion.put("cycleCount", session.cycleLogs().size());
        completion.put("sessionSha256", sha256(directory.resolve("session.json")));
        write("completion.json", completion);
    }

    public void failSetup(Exception error) throws IOException {
        write("completion.json", Map.of("schemaVersion", SCHEMA, "runId", runId,
                "status", "SETUP_FAILED", "exceptionType", error.getClass().getName(),
                "fullBacktestWallNanos", System.nanoTime() - startNanos));
    }

    private void write(String name, Object value) throws IOException {
        Path temp = directory.resolve(name + ".tmp");
        JSON.writeValue(temp.toFile(), value);
        Files.move(temp, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536];
                int length;
                while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static Map<String, String> sourceHashes(Path root) throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        String names = git(root, "ls-files", "--cached", "--others", "--exclude-standard", "-z");
        for (String name : names.split("\u0000")) {
            if (!(name.endsWith(".java") || name.endsWith(".py") || name.equals("algotrader/pom.xml")
                    || name.equals("python-model/requirements.txt") || name.startsWith("config/") && name.endsWith(".json"))) continue;
            Path file = root.resolve(name);
            hashes.put(name, Files.isRegularFile(file) ? sha256(file) : "DELETED");
        }
        return hashes;
    }

    private static String hashClasses(Path directory) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                digest.update(directory.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                digest.update(sha256(file).getBytes(StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String git(Path root, String... args) {
        try {
            List<String> command = new ArrayList<>(List.of("git", "-C", root.toAbsolutePath().toString()));
            command.addAll(List.of(args));
            Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            // These commands only list project metadata, never file contents or remote URLs.
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(10, TimeUnit.SECONDS)) { process.destroyForcibly(); return "UNAVAILABLE"; }
            return process.exitValue() == 0 ? output : "UNAVAILABLE";
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "UNAVAILABLE";
        }
    }
}
