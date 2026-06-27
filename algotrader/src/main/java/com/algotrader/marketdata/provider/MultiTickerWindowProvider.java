package com.algotrader.marketdata.provider;

import java.time.Instant;
import java.util.List;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.DataBatch;

/**
 * Provides model-ready market data windows for multiple tickers at a
 * specified point in time.
 *
 * <p>Implementations attempt to construct a {@link DataBatch} for every
 * configured ticker whose historical data is sufficient to satisfy the
 * required window size. Tickers for which a valid window cannot be
 * constructed are omitted from the returned list.
 *
 * <p>The provider does not advance time or maintain iteration state.
 * Callers are responsible for determining the trading cycle timestamp and
 * invoking {@link #windowsAt(Instant)} as needed.
 */
public interface MultiTickerWindowProvider {

    /**
     * Initializes the provider and prepares any resources required to
     * construct market data windows.
     *
     * @throws DataCacheException if initialization fails
     */
    void initialize() throws DataCacheException;

    /**
     * Constructs all available data windows ending at the specified trading
     * cycle time. cycleTime corresponds to the open time of the last candle
     * to be included in each window.
     *
     * @param cycleTime the timestamp identifying the trading cycle
     * @return a list of successfully constructed data batches, one per ticker;
     *         the list may be empty if no valid windows are available
     * @throws DataCacheException if market data could not be retrieved
     */
    List<DataBatch> windowsAt(Instant cycleTime) throws DataCacheException;
}