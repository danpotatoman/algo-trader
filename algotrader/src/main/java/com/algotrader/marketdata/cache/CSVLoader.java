package com.algotrader.marketdata.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.algotrader.marketdata.model.OHLCV;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Legacy utility for loading OHLCV candlestick data from classpath CSV files.
 *
 * <p>This class reads CSV files from a configured classpath resource
 * directory and parses them into {@link StampedOHLCV} objects. It was used by
 * the earlier file-based market data pipeline before market data was moved
 * toward SQL-backed storage.
 *
 * <p>CSV files are expected to follow the naming convention:
 *
 * <pre>
 * ticker_interval_ohlcv.csv
 * </pre>
 *
 * <p>Each CSV file must contain a header row followed by rows with the format:
 *
 * <pre>
 * timestamp,open,high,low,close,volume
 * </pre>
 *
 * <p>This class performs only file loading and parsing. It does not provide
 * caching, batching, indexing, or database-backed access.
 *
 * <p><b>TODO:</b> Remove this class if the project no longer supports
 * classpath CSV market data loading.
 */
public class CSVLoader {

    private final String dataDirectory;

    /**
     * Creates a CSV loader for a classpath resource directory.
     *
     * @param dataDirectory root directory relative to the classpath containing
     *        CSV files
     * @throws IllegalArgumentException if {@code dataDirectory} is null or blank
     */
    public CSVLoader(String dataDirectory) {
        if (dataDirectory == null || dataDirectory.isBlank()) {
            throw new IllegalArgumentException("Data directory cannot be empty.");
        }

        this.dataDirectory = dataDirectory;
    }

    /**
     * Loads a CSV file and parses it into stamped OHLCV rows.
     *
     * <p>This method resolves files from the classpath, not from an arbitrary
     * filesystem path.
     *
     * @param fileName the name of the CSV file (e.g. {@code "aapl_5m_ohlcv.csv"})
     * @param ticker the ticker symbol associated with the data
     * @param interval the time interval of the candlestick data
     * @return a list of parsed {@link StampedOHLCV} rows in the order they appear in the file
     * @throws DataCacheException if the file cannot be found, read, or parsed
     */
    public List<StampedOHLCV> load(String fileName, String ticker, TimeInterval interval)
            throws DataCacheException {

        String resourcePath = dataDirectory + "/" + fileName;

        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new DataCacheException(
                        "CSV file not found in classpath: " + resourcePath
                );
            }

            return readRows(is, fileName, ticker, interval);

        } catch (IOException e) {
            throw new DataCacheException("Failed to read CSV file: " + fileName, e);
        }
    }

    /**
     * Reads all rows from the given input stream and converts them into
     * {@link StampedOHLCV} objects.
     *
     * <p>This method assumes the first line is a header and skips it.
     *
     * @param is the input stream for the CSV file
     * @param fileName the name of the file (used for error reporting)
     * @param ticker the ticker symbol associated with the data
     * @param interval the time interval of the candlestick data
     * @return a list of parsed rows
     * @throws IOException if an I/O error occurs while reading the stream
     * @throws DataCacheException if the file is empty or contains malformed data
     */
    private List<StampedOHLCV> readRows(
            InputStream is,
            String fileName,
            String ticker,
            TimeInterval interval
    ) throws IOException, DataCacheException {

        List<StampedOHLCV> loadedRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine(); // header

            if (line == null) {
                throw new DataCacheException("CSV file is empty: " + fileName);
            }

            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                loadedRows.add(parseLine(line, lineNumber, ticker, interval));
            }
        }

        return loadedRows;
    }

    /**
     * Parses a single CSV line into a {@link StampedOHLCV} object.
     *
     * <p>The line must contain at least six comma-separated values:
     * timestamp, open, high, low, close, volume.
     *
     * @param line the raw CSV line
     * @param lineNumber the line number in the file (for error reporting)
     * @param ticker the ticker symbol
     * @param interval the time interval
     * @return a parsed {@link StampedOHLCV} object
     * @throws DataCacheException if the line is malformed or contains invalid data
     */
    private StampedOHLCV parseLine(
            String line,
            int lineNumber,
            String ticker,
            TimeInterval interval
    ) throws DataCacheException {

        String[] parts = line.split(",");

        if (parts.length < 6) {
            throw new DataCacheException(
                    "Malformed CSV line " + lineNumber
                            + ": expected 6 columns but got " + parts.length
            );
        }

        try {
            Instant timestamp = parseTimestamp(parts[0]);

            double open = Double.parseDouble(parts[1]);
            double high = Double.parseDouble(parts[2]);
            double low = Double.parseDouble(parts[3]);
            double close = Double.parseDouble(parts[4]);
            long volume = Long.parseLong(parts[5]);

            OHLCV ohlcv = new OHLCV(open, high, low, close, volume);

            return new StampedOHLCV(
                    ticker,
                    interval,
                    timestamp,
                    ohlcv
            );

        } catch (NumberFormatException e) {
            throw new DataCacheException(
                    "Failed to parse numeric value on CSV line " + lineNumber,
                    e
            );
        } catch (IllegalArgumentException e) {
            throw new DataCacheException(
                    "Invalid CSV data on line " + lineNumber + ": " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Parses a timestamp string from the CSV into an {@link Instant}.
     *
     * <p>Supports timestamps of the form:
     *
     * <pre>
     * 2026-04-24 13:30:00+00:00
     * </pre>
     *
     * <p>The method converts the space between date and time into a {@code 'T'}
     * to comply with ISO-8601 parsing.
     *
     * @param rawTimestamp the raw timestamp string from the CSV
     * @return the parsed {@link Instant}
     * @throws IllegalArgumentException if the timestamp is null, blank, or invalid
     */
    private Instant parseTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            throw new IllegalArgumentException("Timestamp cannot be empty.");
        }

        return OffsetDateTime
                .parse(rawTimestamp.trim().replace(" ", "T"))
                .toInstant();
    }
}