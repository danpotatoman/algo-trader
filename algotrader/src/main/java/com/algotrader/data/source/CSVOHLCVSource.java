package com.algotrader.data.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.CSVLoader;
import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.dataobjects.StampedOHLCV;

/**
 * An {@link OHLCVSource} implementation that loads OHLCV candlestick data
 * from CSV files.
 *
 * <p>CSV files are expected to be located under the configured data directory
 * and must follow the naming convention:
 *
 * <pre>
 * ticker_interval_ohlcv.csv
 * </pre>
 *
 * Example:
 * <pre>
 * aapl_5m_ohlcv.csv
 * </pre>
 */
public class CSVOHLCVSource implements OHLCVSource {

    private final CSVLoader csvLoader;

    /**
     * Constructs a CSVOHLCVSource that reads CSV files from the given
     * classpath directory.
     *
     * @param dataDirectory the root directory containing CSV files
     */
    public CSVOHLCVSource(String dataDirectory) {
        this.csvLoader = new CSVLoader(dataDirectory);
    }

    /**
     * Loads all available OHLCV rows for the given ticker and interval.
     *
     * @param ticker the stock ticker symbol
     * @param interval the candlestick interval
     * @return all loaded OHLCV rows
     * @throws DataCacheException if the CSV file cannot be loaded or parsed
     */
    @Override
    public List<StampedOHLCV> loadRows(
            String ticker,
            TimeInterval interval
    ) throws DataCacheException {

        validateRequest(ticker, interval);

        String normalizedTicker = ticker.toUpperCase();
        String fileName = buildFileName(normalizedTicker, interval);

        return csvLoader.load(fileName, normalizedTicker, interval);
    }

    /**
     * Loads a single OHLCV row for the given ticker, interval, and timestamp.
     *
     * <p>This implementation loads the relevant CSV file and searches for an
     * exact timestamp match.
     *
     * @param ticker the stock ticker symbol
     * @param interval the candlestick interval
     * @param timestamp the timestamp of the requested candle
     * @return the matching OHLCV row
     * @throws DataBufferException if the CSV file cannot be loaded or parsed,
     *         or if no matching row exists
     */
    @Override
    public StampedOHLCV loadRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException {

        if (timestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }

        List<StampedOHLCV> rows = loadRows(ticker, interval);

        return rows.stream()
                .filter(row -> row.timestamp().equals(timestamp))
                .findFirst()
                .orElseThrow(() -> new DataCacheException(
                        "No OHLCV row found for ticker "
                                + ticker.toUpperCase()
                                + ", interval "
                                + interval
                                + ", timestamp "
                                + timestamp
                ));
    }

    @Override //TODO: implement
    public Optional<Instant> getNextTimestamp(String ticker, TimeInterval interval, Instant timestamp) {
        return null;
    }
    @Override //TODO: implement
    public List<StampedOHLCV> loadRange(String ticker, TimeInterval interval, Instant startTime, Instant endTime){
        return null;
    }

    /**
     * Returns true because this source reads from CSV files.
     *
     * @return {@code true}
     */
    @Override
    public boolean isFileBased() {
        return true;
    }

    /**
     * Returns false because this source reads static CSV data.
     *
     * @return {@code false}
     */
    @Override
    public boolean isLiveData() {
        return false;
    }

    private void validateRequest(String ticker, TimeInterval interval)
            throws DataCacheException {

        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException("Ticker cannot be empty.");
        }

        if (interval == null) {
            throw new DataCacheException("Interval cannot be null.");
        }
    }

    private String buildFileName(String ticker, TimeInterval interval) {
        return ticker.toLowerCase() + "_" + intervalToFileToken(interval) + "_ohlcv.csv";
    }

    private String intervalToFileToken(TimeInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> "1m";
            case FIVE_MINUTES -> "5m";
            case FIFTEEN_MINUTES -> "15m";
            case THIRTY_MINUTES -> "30m";
            case ONE_HOUR -> "1h";
            case ONE_DAY -> "1d";
        };
    }
}