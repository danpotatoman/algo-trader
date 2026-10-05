package com.algotrader.logging;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.algotrader.decision.prediction.api.PythonPredictionClient;
import com.algotrader.decision.prediction.api.request.BatchClassificationVolatilityRequest;
import com.algotrader.decision.prediction.provider.*;
import com.algotrader.marketdata.model.TimeInterval;

class PredictionProvenanceTest {
    @Test void serviceRestartAndDuplicateTickerResponsesAreRejected() throws Exception {
        String id = UUID.randomUUID().toString();
        var response = new AtomicReference<>("{\"serverInstanceId\":\"" + id + "\",\"predictions\":[{\"ticker\":\"AAPL\",\"probability\":0.9,\"volatility\":0.01,\"horizonMinutes\":30}]}");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String metadata = "{\"serverInstanceId\":\"" + id + "\",\"batchEndpoint\":\"/predict/test\",\"models\":[{\"sha256\":\"" + "a".repeat(64) + "\"},{\"sha256\":\"" + "b".repeat(64) + "\"}]}";
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = (exchange.getRequestURI().getPath().equals("/provenance") ? metadata : response.get()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
        server.start();
        try {
            var provider = new PythonBatchClassificationWithVolatilityPredictionProvider(
                    new PythonPredictionClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/predict/test")),
                    input -> new BatchClassificationVolatilityRequest(List.of()), TimeInterval.FIVE_MINUTES);
            assertEquals(id, provider.provenance().path("serverInstanceId").asText());
            assertEquals(1, provider.predict(List.of(DecisionObservabilityTest.batch("AAPL"))).size());
            response.set(response.get().replace(id, UUID.randomUUID().toString()));
            assertThrows(PredictionProviderException.class, () -> provider.predict(List.of(DecisionObservabilityTest.batch("AAPL"))));
            response.set("{\"serverInstanceId\":\"" + id + "\",\"predictions\":[{\"ticker\":\"AAPL\",\"probability\":0.9,\"volatility\":0.01,\"horizonMinutes\":30},{\"ticker\":\"AAPL\",\"probability\":0.9,\"volatility\":0.01,\"horizonMinutes\":30}]}");
            assertThrows(PredictionProviderException.class, () -> provider.predict(List.of(
                    DecisionObservabilityTest.batch("AAPL"), DecisionObservabilityTest.batch("MSFT"))));
        } finally { server.stop(0); }
    }
}
