package com.algotrader.marketdata.provider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Default implementation of {@link MultiTickerWindowProvider}.
 *
 * <p>This provider constructs model-ready {@link DataBatch} windows for a
 * configured set of tickers at caller-supplied cycle timestamps.
 *
 * <p>The provider does not advance time. Callers are responsible for deciding
 * which timestamps should be evaluated.
 *
 * <p>During initialization, the provider preloads the configured candle range
 * into the underlying {@link MarketDataProvider}. Timestamps outside of that
 * range are still supported by {@link #windowsAt(Instant)}, but may require
 * slower on-demand loading by the underlying provider.
 */
public final class DefaultMultiTickerWindowProvider
        implements MultiTickerWindowProvider {

    private final List<String> tickers;
    private final TimeInterval interval;
    private final int windowSize;
    private final MarketDataProvider marketDataProvider;
    private final Instant preloadStartInclusive;
    private final Instant preloadEndInclusive;

    /**
     * Creates a provider without a preload range.
     *
     * @param tickers ticker symbols to include in each cycle
     * @param interval candle interval used to build windows
     * @param windowSize number of candles required per window
     * @param marketDataProvider provider used to retrieve OHLCV data
     * @throws IllegalArgumentException if any argument is invalid
     */
    public DefaultMultiTickerWindowProvider(
            List<String> tickers,
            TimeInterval interval,
            int windowSize,
            MarketDataProvider marketDataProvider) {

        validateCommonConstructorArgs(tickers, interval, windowSize, marketDataProvider);

        this.tickers = List.copyOf(tickers);
        this.interval = interval;
        this.windowSize = windowSize;
        this.marketDataProvider = marketDataProvider;
        this.preloadStartInclusive = null;
        this.preloadEndInclusive = null;
    }

    /**
     * Creates a provider with an optional preload range.
     *
     * @param tickers ticker symbols to include in each cycle
     * @param interval candle interval used to build windows
     * @param windowSize number of candles required per window
     * @param marketDataProvider provider used to retrieve OHLCV data
     * @param preloadStartInclusive first candle timestamp to preload
     * @param preloadEndInclusive last candle timestamp to preload
     * @throws IllegalArgumentException if any argument is invalid
     */
    public DefaultMultiTickerWindowProvider(
            List<String> tickers,
            TimeInterval interval,
            int windowSize,
            MarketDataProvider marketDataProvider,
            Instant preloadStartInclusive,
            Instant preloadEndInclusive) {

        validateCommonConstructorArgs(tickers, interval, windowSize, marketDataProvider);

        if ((preloadStartInclusive == null) != (preloadEndInclusive == null)) {
            throw new IllegalArgumentException(
                    "Preload start and end must either both be provided or both be null.");
        }

        if (preloadStartInclusive != null
                && preloadStartInclusive.isAfter(preloadEndInclusive)) {
            throw new IllegalArgumentException(
                    "Preload start cannot be after preload end.");
        }

        this.tickers = List.copyOf(tickers);
        this.interval = interval;
        this.windowSize = windowSize;
        this.marketDataProvider = marketDataProvider;
        this.preloadStartInclusive = preloadStartInclusive;
        this.preloadEndInclusive = preloadEndInclusive;
    }

    @Override
    public void initialize() throws DataCacheException {
        if (preloadStartInclusive == null || preloadEndInclusive == null) {
            return;
        }

        marketDataProvider.preloadSession(
                tickers,
                interval,
                preloadStartInclusive,
                preloadEndInclusive);
    }

    /**
     * Returns valid windows containing only candles completed by the cycle time.
     *
     * @param cycleTime decision and execution timestamp, aligned to the candle interval
     * @return model-ready data batches for tickers with complete windows
     * @throws DataCacheException if market data retrieval fails
     */
    @Override
    public List<DataBatch> windowsAt(Instant cycleTime)
            throws DataCacheException {

        if (cycleTime == null) {
            throw new DataCacheException("Cycle time cannot be null.");
        }

        List<DataBatch> batches = new ArrayList<>();

        Instant lastCompletedCandleOpen = cycleTime.minus(interval.getDuration());
        Instant windowStartTime = calculateWindowStartTime(lastCompletedCandleOpen);

        for (String ticker : tickers) {
            List<StampedOHLCV> rows = marketDataProvider.requestRange(
                    ticker,
                    interval,
                    windowStartTime,
                    lastCompletedCandleOpen);

            if (!isValidWindow(rows, windowStartTime, lastCompletedCandleOpen)) {
                continue;
            }

            batches.add(new DataBatch(interval, rows));
        }

        return batches;
    }

    private Instant calculateWindowStartTime(Instant lastCompletedCandleOpen) {
        Duration windowOffset = interval.getDuration()
                .multipliedBy(windowSize - 1L);

        return lastCompletedCandleOpen.minus(windowOffset);
    }

    private boolean isValidWindow(
            List<StampedOHLCV> rows,
            Instant expectedStartTime,
            Instant expectedEndTime) {

        if (rows == null || rows.size() != windowSize) {
            return false;
        }

        for (int i = 0; i < rows.size(); i++) {
            Instant expectedTimestamp = expectedStartTime.plus(
                    interval.getDuration().multipliedBy(i));

            Instant actualTimestamp = rows.get(i).candleOpenTime();

            if (!expectedTimestamp.equals(actualTimestamp)) {
                return false;
            }
        }

        Instant finalTimestamp = rows.get(rows.size() - 1).candleOpenTime();

        return expectedEndTime.equals(finalTimestamp);
    }

    private void validateCommonConstructorArgs(
            List<String> tickers,
            TimeInterval interval,
            int windowSize,
            MarketDataProvider marketDataProvider) {
        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("Tickers cannot be empty.");
        }

        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive.");
        }

        if (marketDataProvider == null) {
            throw new IllegalArgumentException(
                    "Market data provider cannot be null.");
        }
    }
}
