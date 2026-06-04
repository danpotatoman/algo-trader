package com.algotrader.marketdata.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.persistence.OHLCVRepository;

public class SQLiteOHLCVSource implements OHLCVSource {

    private final OHLCVRepository repository;

    public SQLiteOHLCVSource(OHLCVRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }

        this.repository = repository;
    }

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