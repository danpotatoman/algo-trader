package com.algotrader.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.net.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.*;
import com.algotrader.decision.prediction.provider.*;
import com.algotrader.decision.generator.ClassificationVolatilityPortfolioDecisionGenerator;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.portfolio.PortfolioSnapshot;

class VolatilityUnitsIntegrationTest {
    @Test void pythonRoutesThroughJavaMappingAndAllocationPreserveCanonicalUnits() throws Exception {
        String python = System.getenv("VOLATILITY_TEST_PYTHON");
        assumeTrue(python != null, "Set VOLATILITY_TEST_PYTHON to run the Python/Java contract integration test");
        var process = new ProcessBuilder(python, "tests/test_volatility_units.py", "--emit-probe")
                .directory(Path.of("../python-model").toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Python endpoint probe timed out");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            var probe = new ObjectMapper().readTree(output);
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                String path = exchange.getRequestURI().getPath();
                String key = path.equals("/provenance") ? "provenance" : path.endsWith("/batch") ? "batch" : "single";
                byte[] body = probe.get(key).toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var stream = exchange.getResponseBody()) { stream.write(body); }
            });
            server.start();
            try {
                String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/predict/cnn-classification-volatility-v1";
                var single = new PythonClassificationWithVolatilityPredictionProvider(new PythonPredictionClient(URI.create(base)),
                        input -> new ClassificationWithVolatilityRequest("AAPL", List.of()), TimeInterval.FIVE_MINUTES);
                var batch = new PythonBatchClassificationWithVolatilityPredictionProvider(new PythonPredictionClient(URI.create(base + "/batch")),
                        input -> new BatchClassificationVolatilityRequest(List.of()), TimeInterval.FIVE_MINUTES);
                var inputs = List.of(DecisionObservabilityTest.batch("AAPL"), DecisionObservabilityTest.batch("MSFT"));
                assertEquals(.001, single.predict(inputs.get(0)).volatility(), 1e-9);
                var predictions = batch.predict(inputs);
                for (var prediction : predictions) assertEquals(.001, prediction.volatility(), 1e-9);
                assertEquals("return_fraction", batch.provenance().path("volatilityOutputContract").path("units").asText());
                Instant time = Instant.parse("2026-05-18T14:05:00Z");
                var generator = new ClassificationVolatilityPortfolioDecisionGenerator(batch, .25, .4, .2,
                        HistoricalRunArtifactTest.policy(time.plusSeconds(3600)), "unit-test");
                var result = generator.generateDecision(inputs, new PortfolioSnapshot(1000, Map.of()), List.of(), time);
                assertEquals(2, result.newCapitalAllocations().size());
                for (var signal : result.signals()) {
                    assertEquals((signal.probability() - .25) / 1.001, signal.score(), 1e-9);
                    assertEquals(200, signal.requestedCash(), 1e-8);
                }
            } finally { server.stop(0); }
        } finally { process.destroyForcibly(); }
    }
}
