package com.algotrader.marketdata.provider;

import java.time.Instant;

import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.MarketPrice;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * A {@link PriceProvider} implementation backed by a
 * {@link MarketDataProvider}.
 *
 * <p>This provider uses a fixed {@link TimeInterval} configured at
 * construction time. All price lookups are delegated to the underlying
 * provider using that interval.
 *
 * <p>This is useful when a trading system or strategy operates entirely
 * on a single interval and should not need to repeatedly specify it.
 */
public class CachedPriceProvider implements PriceProvider {

    private final MarketDataProvider marketDataProvider;
    private final TimeInterval interval;

    /**
     * Constructs a {@code CachedPriceProvider}.
     *
     * @param marketDataCache the backing market data cache
     * @param interval the fixed interval used for all lookups
     *
     * @throws IllegalArgumentException if either argument is null
     */
    public CachedPriceProvider(
            MarketDataProvider marketDataProvider,
            TimeInterval interval
    ) {
        if (marketDataProvider == null) {
            throw new IllegalArgumentException(
                    "MarketDataCache cannot be null."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "TimeInterval cannot be null."
            );
        }

        this.marketDataProvider = marketDataProvider;
        this.interval = interval;
    }

    /**
     * Retrieves the market price for a ticker at the specified timestamp
     * using this provider's configured interval.
     *
     * @param ticker the ticker symbol to query
     * @param timestamp the exact lookup timestamp
     * @return market price information for the exact requested timestamp
     */
    @Override
    public MarketPrice getTickerPrice(
            String ticker,
            Instant timestamp
    ) throws DataCacheException {
        StampedOHLCV row = marketDataProvider.requestRow(
                ticker,
                interval,
                timestamp
        );

        return new MarketPrice(
                ticker.toUpperCase(),
                row.close(),
                row.timestamp()
        );
    }

    /**
     * Returns the fixed interval used by this provider.
     *
     * @return the configured interval
     */
    public TimeInterval getInterval() {
        return interval;
    }
}