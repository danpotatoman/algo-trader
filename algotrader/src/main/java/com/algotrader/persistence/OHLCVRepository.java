package com.algotrader.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Repository abstraction for persistent OHLCV market data storage.
 *
 * <p>An {@code OHLCVRepository} provides CRUD-style access to
 * {@link StampedOHLCV} candles independent of the underlying storage
 * mechanism.
 *
 * <p>Implementations may be backed by SQLite, PostgreSQL, flat files,
 * cloud storage, or other persistence technologies.
 *
 * <p>Repository methods operate on fully normalized market data identified
 * by:
 * <ul>
 *     <li>Ticker symbol</li>
 *     <li>Time interval</li>
 *     <li>Timestamp</li>
 * </ul>
 *
 * <p>This interface is intended for persistence concerns only and should
 * not contain caching, batching, sliding-window traversal, prediction,
 * or trading logic.
 */
public interface OHLCVRepository {

        /**
         * Saves a single OHLCV candle.
         *
         * <p>If a candle already exists for the same ticker, interval, and
         * timestamp, implementations may update the existing record.
         *
         * @param candle candle to persist
         */
        void save(StampedOHLCV candle);

        /**
         * Saves multiple OHLCV candles.
         *
         * <p>Implementations may optimize this operation using batch inserts
         * or transactions.
         *
         * @param candles candles to persist
         */
        void saveAll(List<StampedOHLCV> candles);

        /**
         * Retrieves a candle by its full primary key.
         *
         * @param ticker ticker symbol
         * @param interval candle interval
         * @param timestamp exact candle timestamp
         * @return matching candle, or {@link Optional#empty()} if none exists
         */
        Optional<StampedOHLCV> findByKey(
                String ticker,
                TimeInterval interval,
                Instant timestamp
        );

        /**
         * Retrieves all candles within an inclusive timestamp range.
         *
         * @param ticker ticker symbol
         * @param interval candle interval
         * @param start inclusive range start
         * @param end inclusive range end
         * @return matching candles ordered by ascending timestamp
         */
        List<StampedOHLCV> findRange(
                String ticker,
                TimeInterval interval,
                Instant start,
                Instant end
        );

        /**
         * Retrieves all stored candles for a ticker and interval.
         *
         * @param ticker ticker symbol
         * @param interval candle interval
         * @return matching candles ordered by ascending timestamp
         */
        List<StampedOHLCV> findAll(
                String ticker,
                TimeInterval interval
        );

        /**
         * Retrieves the most recent stored candle for a ticker and interval.
         *
         * @param ticker ticker symbol
         * @param interval candle interval
         * @return latest candle, or {@link Optional#empty()} if none exists
         */
        Optional<StampedOHLCV> findLatest(
                String ticker,
                TimeInterval interval
        );

        /**
         * Retrieves the next available timestamp after the supplied timestamp.
         *
         * @param ticker ticker symbol
         * @param interval candle interval
         * @param timestamp timestamp after which to search
         * @return next available timestamp, or {@link Optional#empty()} if none exists
         */
        Optional<Instant> findNextTimestamp(
                String ticker,
                TimeInterval interval,
                Instant timestamp
        );
}