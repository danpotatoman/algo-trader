package com.algotrader.marketdata.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.persistence.OHLCVRepository;

/**
 * {@link OHLCVSource} implementation backed by a SQLite OHLCV repository.
 *
 * <p>This source adapts the persistence layer to the market data source
 * abstraction. It delegates OHLCV queries to an {@link OHLCVRepository} and
 * wraps repository failures in {@link DataCacheException} so higher-level
 * market data components do not depend on persistence-specific exceptions.
 *
 * <p>This source provides historical database-backed data. It is not
 * file-based and does not represent a live/updating market data feed.
 */
public final class SQLiteOHLCVSource implements OHLCVSource {
//TODO: lots of DataCacheExceptions are thrown here. Probably should use DataCacheException(message, e) and pass e along
    private final OHLCVRepository repository;

    /**
     * Creates a SQLite-backed OHLCV source.
     *
     * @param repository repository used to query persisted OHLCV data
     * @throws IllegalArgumentException if {@code repository} is null
     */
    public SQLiteOHLCVSource(OHLCVRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }

        this.repository = repository;
    }

    /**
     * Loads all persisted OHLCV rows for a ticker and interval.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @return all matching rows
     * @throws DataCacheException if the request is invalid or the repository
     *         query fails
     */
    @Override
    public List<StampedOHLCV> loadRows(
            String ticker,
            TimeInterval interval
    ) throws DataCacheException {
        validateTickerAndInterval(ticker, interval);

        try {
            return repository.findAll(ticker, interval);
        } catch (Exception e) {
            throw new DataCacheException(
                    "Failed to load OHLCV rows for "
                            + ticker + " " + interval
            );
        }
    }

    /**
     * Loads persisted OHLCV rows within an inclusive timestamp range.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param startTimestamp inclusive range start
     * @param endTimestamp inclusive range end
     * @return matching rows
     * @throws DataCacheException if the request is invalid or the repository
     *         query fails
     */
    @Override
    public List<StampedOHLCV> loadRange(
            String ticker,
            TimeInterval interval,
            Instant startTimestamp,
            Instant endTimestamp
    ) throws DataCacheException {
        validateTickerAndInterval(ticker, interval);

        if (startTimestamp == null) {
            throw new DataCacheException("startTimestamp cannot be null");
        }

        if (endTimestamp == null) {
            throw new DataCacheException("endTimestamp cannot be null");
        }

        if (startTimestamp.isAfter(endTimestamp)) {
            throw new DataCacheException(
                    "startTimestamp cannot be after endTimestamp"
            );
        }

        try {
            return repository.findRange(
                    ticker,
                    interval,
                    startTimestamp,
                    endTimestamp
            );
        } catch (Exception e) {
            throw new DataCacheException(
                    "Failed to load OHLCV range for "
                            + ticker + " " + interval
                            + " from " + startTimestamp
                            + " to " + endTimestamp
            );
        }
    }

    /**
     * Loads a single persisted OHLCV row at an exact timestamp.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param timestamp exact candle timestamp
     * @return matching OHLCV row
     * @throws DataCacheException if the request is invalid, no matching row
     *         exists, or the repository query fails
     */
    @Override
    public StampedOHLCV loadRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException {
        validateTickerAndInterval(ticker, interval);

        if (timestamp == null) {
            throw new DataCacheException("timestamp cannot be null");
        }

        try {
            return repository.findByKey(ticker, interval, timestamp)
                    .orElseThrow(() -> new DataCacheException(
                            "No OHLCV row found for "
                                    + ticker + " " + interval
                                    + " at " + timestamp
                    ));
        } catch (DataCacheException e) {
            throw e;
        } catch (Exception e) {
            throw new DataCacheException(
                    "Failed to load OHLCV row for "
                            + ticker + " " + interval
                            + " at " + timestamp
            );
        }
    }

    /**
     * Returns the next persisted candle timestamp after the supplied timestamp.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param timestamp timestamp after which to search
     * @return next available timestamp, or {@link Optional#empty()} if none
     *         exists
     * @throws DataCacheException if the request is invalid or the repository
     *         query fails
     */
    @Override
    public Optional<Instant> getNextTimestamp(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataCacheException {

        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException(
                    "ticker cannot be null or blank"
            );
        }

        if (interval == null) {
            throw new DataCacheException(
                    "interval cannot be null"
            );
        }

        if (timestamp == null) {
            throw new DataCacheException(
                    "timestamp cannot be null"
            );
        }

        try {
            return repository.findNextTimestamp(
                    ticker,
                    interval,
                    timestamp
            );

        } catch (Exception e) {
            throw new DataCacheException(
                    "Failed to retrieve next timestamp for "
                            + ticker
                            + " "
                            + interval
                            + " after "
                            + timestamp
            );
        }
    }

    @Override
    public boolean isFileBased() {
        return false;
    }

    @Override
    public boolean isLiveData() {
        return false;
    }

    

    private void validateTickerAndInterval(
            String ticker,
            TimeInterval interval
    ) throws DataCacheException {
        if (ticker == null || ticker.isBlank()) {
            throw new DataCacheException("ticker cannot be null or blank");
        }

        if (interval == null) {
            throw new DataCacheException("interval cannot be null");
        }
    }
}