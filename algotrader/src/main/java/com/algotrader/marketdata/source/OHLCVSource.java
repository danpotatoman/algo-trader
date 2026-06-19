package com.algotrader.marketdata.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Low-level source of OHLCV candlestick data.
 *
 * <p>An {@code OHLCVSource} represents a concrete data source such as a
 * database, CSV file, external API, synthetic generator, or live market data
 * feed.
 *
 * <p>This interface sits below {@link com.algotrader.marketdata.provider.MarketDataProvider}
 * in the market data architecture. Implementations are responsible only for
 * loading raw stamped OHLCV rows from their backing source.
 *
 * <p>This interface does not provide caching, batching, sliding-window
 * traversal, prediction logic, or trade execution behavior. Those
 * responsibilities belong to higher-level components.
 */
public interface OHLCVSource {

        /**
         * Loads all available OHLCV rows for a ticker and interval.
         *
         * @param ticker ticker symbol to query
         * @param interval candle interval to query
         * @return all available matching rows
         * @throws DataCacheException if the data cannot be retrieved
         */
        List<StampedOHLCV> loadRows(
                String ticker,
                TimeInterval interval
        ) throws DataCacheException;

        /**
         * Loads all OHLCV rows within the requested time range.
         *
         * <p>The start and end candle timestamps are inclusive. Returned rows
         * should be ordered by ascending timestamp.
         *
         * @param ticker ticker symbol to query
         * @param interval candle interval to query
         * @param firstCandleTimestamp inclusive first candle timestamp
         * @param lastCandleTimestamp inclusive last candle timestamp
         * @return matching rows ordered by timestamp
         * @throws DataCacheException if the data cannot be retrieved
         */
        List<StampedOHLCV> loadRange(
                String ticker,
                TimeInterval interval,
                Instant firstCandleTimestamp,
                Instant lastCandleTimestamp
        ) throws DataCacheException;

        /**
         * Loads a single OHLCV row at an exact candle timestamp.
         *
         * <p>The timestamp must exactly match an available candle timestamp. No
         * nearest-neighbor lookup or interpolation is performed.
         *
         * @param ticker ticker symbol to query
         * @param interval candle interval to query
         * @param candleTimestamp exact candle timestamp
         * @return matching OHLCV row
         * @throws DataCacheException if the row cannot be retrieved or no matching
         *         candle exists
         */
        StampedOHLCV loadRow(
                String ticker,
                TimeInterval interval,
                Instant candleTimestamp
        ) throws DataCacheException;

        /**
         * Returns the next available candle timestamp after the supplied timestamp.
         *
         * <p>This method is useful for traversing market data when calendar time
         * does not map cleanly to available candle timestamps, such as around
         * weekends, holidays, market closures, or missing data.
         *
         * @param ticker ticker symbol to query
         * @param interval candle interval to query
         * @param timestamp timestamp after which to search
         * @return the next available timestamp, or {@link Optional#empty()} if none
         *         exists
         * @throws DataCacheException if the lookup cannot be performed
         */
        Optional<Instant> getNextCandleTimestamp(
                        String ticker,
                        TimeInterval interval,
                        Instant timestamp
                ) throws DataCacheException;

        /**
         * Returns whether this source retrieves data from files.
         *
         * <p>Examples include CSV-backed or binary file-backed sources.
         *
         * @return {@code true} if this source is file-based
         */
        boolean isFileBased();

        /**
         * Returns whether this source retrieves live/updating market data.
         *
         * <p>Examples include polling APIs or websocket-backed sources.
         *
         * @return {@code true} if this source provides live market data
         */
        boolean isLiveData();
}