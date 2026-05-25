package com.algotrader.data.source;

import java.time.Instant;
import java.util.List;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.buffer.DataBufferException;
import com.algotrader.data.dataobjects.StampedOHLCV;

/**
 * Provides low-level OHLCV candlestick data retrieval.
 *
 * <p>Implementations may retrieve data from CSV files, APIs,
 * databases, synthetic generators, or other sources.
 *
 * <p>This interface is intentionally lower-level than
 * MarketDataProvider and does not provide batching,
 * sliding-window traversal, caching, or price lookup logic.
 */
public interface OHLCVSource {

    /**
     * Retrieves all available OHLCV rows for a ticker and interval.
     *
     * @param ticker the stock ticker symbol, e.g. {@code "AAPL"}
     * @param interval the candlestick interval
     * @return all available matching OHLCV rows
     * @throws DataBufferException if the data cannot be retrieved
     */
    List<StampedOHLCV> loadRows(
            String ticker,
            TimeInterval interval
    ) throws DataBufferException;

    /**
     * Retrieves a single OHLCV row for a ticker, interval, and timestamp.
     *
     * @param ticker the stock ticker symbol, e.g. {@code "AAPL"}
     * @param interval the candlestick interval
     * @param timestamp the timestamp of the requested candle
     * @return the matching OHLCV row
     * @throws DataBufferException if the data cannot be retrieved
     *         or no matching candle exists
     */
    StampedOHLCV loadRow(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) throws DataBufferException;

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