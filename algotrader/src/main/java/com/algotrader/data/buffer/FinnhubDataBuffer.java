// package com.algotrader.data.buffer;

// import java.io.IOException;
// import java.net.URI;
// import java.net.URLEncoder;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.nio.charset.StandardCharsets;
// import java.time.Instant;
// import java.util.ArrayList;
// import java.util.List;

// import com.algotrader.data.TimeInterval;
// import com.algotrader.data.dataobjects.DataBatch;
// import com.algotrader.data.dataobjects.OHLCV;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// public class FinnhubDataBuffer implements DataBuffer {

//     private static final String BASE_URL = "https://finnhub.io/api/v1/stock/candle";

//     private final String apiKey = System.getenv("FINNHUB_API_KEY");
//     private final String ticker;
//     private final TimeInterval interval;
//     private final int batchSize;

//     private final HttpClient httpClient;
//     private final ObjectMapper objectMapper;

//     public FinnhubDataBuffer(String ticker, TimeInterval interval, int batchSize) {
//         if (ticker == null || ticker.isBlank()) {
//             throw new IllegalArgumentException("Ticker cannot be blank.");
//         }
//         if (interval == null) {
//             throw new IllegalArgumentException("Time interval cannot be null.");
//         }
//         if (batchSize <= 0) {
//             throw new IllegalArgumentException("Batch size must be positive.");
//         }

//         this.ticker = ticker.toUpperCase();
//         this.interval = interval;
//         this.batchSize = batchSize;

//         this.httpClient = HttpClient.newHttpClient();
//         this.objectMapper = new ObjectMapper();
//     }

//     @Override
//     public DataBatch requestBatch(String ticker, TimeInterval timeInterval, int batchSize) throws DataBufferException {
//         long to = Instant.now().getEpochSecond();

//         // Add one extra interval as a small buffer in case the newest candle is incomplete/missing.
//         long secondsBack = (long) interval.getSeconds() * (batchSize + 1);
//         long from = to - secondsBack;

//         String url = buildUrl(from, to);

//         try {
//             HttpRequest request = HttpRequest.newBuilder()
//                     .uri(URI.create(url))
//                     .GET()
//                     .build();

//             HttpResponse<String> response = httpClient.send(
//                     request,
//                     HttpResponse.BodyHandlers.ofString()
//             );

//             if (response.statusCode() != 200) {
//                 throw new DataBufferException(
//                         "Finnhub request failed with status " + response.statusCode()
//                                 + ": " + response.body()
//                 );
//             }

//             return parseDataBatch(response.body());

//         } catch (IOException e) {
//             throw new DataBufferException("Network error while calling Finnhub.", e);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             throw new DataBufferException("Finnhub request was interrupted.", e);
//         }
//     }

//     private String buildUrl(long from, long to) {
//         return BASE_URL
//                 + "?symbol=" + encode(ticker)
//                 + "&resolution=" + encode(interval.getFinnhubResolution())
//                 + "&from=" + from
//                 + "&to=" + to
//                 + "&token=" + encode(apiKey);
//     }

//     private DataBatch parseDataBatch(String json) throws DataBufferException {
//         try {
//             JsonNode root = objectMapper.readTree(json);

//             String status = root.path("s").asText();

//             if (!"ok".equals(status)) {
//                 throw new DataBufferException("Finnhub returned no candle data: " + json);
//             }

//             JsonNode opens = root.get("o");
//             JsonNode highs = root.get("h");
//             JsonNode lows = root.get("l");
//             JsonNode closes = root.get("c");
//             JsonNode volumes = root.get("v");

//             int count = closes.size();

//             if (count < batchSize) {
//                 throw new DataBufferException(
//                         "Finnhub returned only " + count + " candles, but batch size is " + batchSize
//                 );
//             }

//             List<OHLCV> bars = new ArrayList<>();

//             // Take the most recent batchSize candles.
//             int start = count - batchSize;

//             for (int i = start; i < count; i++) {
//                 double open = opens.get(i).asDouble();
//                 double high = highs.get(i).asDouble();
//                 double low = lows.get(i).asDouble();
//                 double close = closes.get(i).asDouble();
//                 long volume = volumes.get(i).asLong();

//                 bars.add(new OHLCV(open, high, low, close, volume));
//             }

//             return new DataBatch(ticker, batchSize, bars, Instant.now()); //constructor for DataBatch has changed
//         } catch (DataBufferException e) {
//             throw e;
//         } catch (Exception e) {
//             throw new DataBufferException("Failed to parse Finnhub candle response.", e);
//         }
//     }

//     private String encode(String value) {
//         return URLEncoder.encode(value, StandardCharsets.UTF_8);
//     }
// }