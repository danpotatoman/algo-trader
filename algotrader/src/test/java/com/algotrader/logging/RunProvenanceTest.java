package com.algotrader.logging;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.algotrader.config.*;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.runtime.ResolvedTradingPlan;

class RunProvenanceTest {
    @TempDir Path temp;

    @Test void snapshotsAreIsolatedAndManifestRedactsCredentials() throws Exception {
        Files.createDirectories(temp.resolve("data"));
        Path source = temp.resolve("data/ohlcv.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + source);
             var sql = connection.createStatement()) {
            sql.execute("PRAGMA journal_mode=WAL");
            sql.execute("CREATE TABLE marker (value INTEGER)");
            sql.execute("INSERT INTO marker VALUES (1)");
            var first = new RunArtifacts(temp.resolve("runs"));
            var second = new RunArtifacts(temp.resolve("runs"));
            assertNotEquals(first.runId(), second.runId());
            first.prepare(temp, plan());
            sql.execute("UPDATE marker SET value=2");
            try (var snapshot = DriverManager.getConnection("jdbc:sqlite:" + first.database());
                 var query = snapshot.createStatement(); var rows = query.executeQuery("SELECT value FROM marker")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            String raw = Files.readString(first.directory().resolve("manifest.json"));
            for (String secret : List.of("secret-user", "secret-password", "secret-token", "secret-description", "secret-key")) {
                assertFalse(raw.contains(secret));
            }
            var manifest = new ObjectMapper().readTree(raw);
            assertEquals(RunArtifacts.sha256(first.database()), manifest.path("data").path("sha256").asText());
            assertEquals(31, manifest.path("endpoint").path("numCandles").asInt());
            assertEquals(0.25, manifest.path("strategy").path("parameters").path("minConfidenceThreshold").asDouble());
        }
    }

    private ResolvedTradingPlan plan() {
        Instant start = Instant.parse("2026-05-18T14:05:00Z");
        return new ResolvedTradingPlan(new TradingSessionConfig("test", "endpoint", "strategy", List.of("AAPL"),
                start, start.plusSeconds(300), 1000),
                new EndpointConfig("endpoint", PredictionType.BATCH_CLASSIFICATION_WITH_VOLATILITY,
                        URI.create("http://secret-user:secret-password@localhost:8000/predict/test?token=secret-token"),
                        "secret-description", "1", TimeInterval.FIVE_MINUTES, 31, Map.of()),
                new TradeGeneratorConfig("strategy", TradeGeneratorType.VOLATILITY_SCALED_THRESHOLD, "test", "1",
                        new TradeGeneratorConfig.StrategyParameters(Map.of("minConfidenceThreshold", 0.25, "apiKey", "secret-key"))));
    }
}
