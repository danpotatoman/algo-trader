package com.algotrader.marketdata.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.cache.CSVLoader;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Legacy {@link OHLCVSource} implementation that loads OHLCV data from
 * classpath CSV files.
 *
 * <p>This source was part of the original file-based market data pipeline,
 * where historical candles were read from CSV resources using
 * {@link CSVLoader}. The project has since moved toward a cache/source
 * architecture backed by more robust data storage, so this class should be
 * treated as legacy infrastructure.
 *
 * <p>CSV files are expected to follow the naming convention:
 *
 * <pre>
 * ticker_interval_ohlcv.csv
 * </pre>
 *
 * <p>For example:
 *
 * <pre>
 * aapl_5m_ohlcv.csv
 * </pre>
 *
 * <p><b>TODO:</b> Remove this class if CSV-backed OHLCV loading is no longer
 * supported.
 *
 * <p><b>TODO:</b> This class does not currently implement all
 * {@link OHLCVSource} methods and should not be used in the current market
 * data pipeline without completing or removing those methods.
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
     * Loads a single OHLCV row for the given ticker, interval, and candle timestamp.
     *
     * <p>This implementation loads the relevant CSV file and searches for an
     * exact candle timestamp match.
     *
     * @param ticker the stock ticker symbol
     * @param interval the candlestick interval
     * @param candleTimestamp the timestamp of the requested candle
     * @return the matching OHLCV row
     * @throws DataCacheException if the CSV file cannot be loaded or parsed,
     *         or if no matching row exists
     */
    @Override
    public StampedOHLCV loadRow(
            String ticker,
            TimeInterval interval,
            Instant candleTimestamp
    ) throws DataCacheException {

        if (candleTimestamp == null) {
            throw new DataCacheException("Timestamp cannot be null.");
        }

        List<StampedOHLCV> rows = loadRows(ticker, interval);

        return rows.stream()
                .filter(row -> row.candleOpenTime().equals(candleTimestamp))
                .findFirst()
                .orElseThrow(() -> new DataCacheException(
                        "No OHLCV row found for ticker "
                                + ticker.toUpperCase()
                                + ", interval "
                                + interval
                                + ", candleTimestamp "
                                + candleTimestamp
                ));
    }

    /**
     * Legacy placeholder. Next timestamp retrieval is not implemented for this CSV source.
     *
     * @throws UnsupportedOperationException if called
     */
    @Override //TODO: implement
    public Optional<Instant> getNextCandleTimestamp(String ticker, TimeInterval interval, Instant timestamp) {
        throw new UnsupportedOperationException("Next timestamp retrieval is not implemented for this CSV source.");
    }

    /**
     * Legacy placeholder. Range loading is not implemented for this CSV source.
     *
     * @throws UnsupportedOperationException if called
     */
    @Override
    public List<StampedOHLCV> loadRange(String ticker, TimeInterval interval, Instant startTime, Instant endTime){
        throw new UnsupportedOperationException("Range loading is not implemented for this CSV source.");
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