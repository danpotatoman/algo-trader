package com.algotrader.data.cache;

import java.time.Instant;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.MarketDataCache;
import com.algotrader.data.dataobjects.MarketPrice;

/**
 * A {@link PriceProvider} implementation backed by a
 * {@link MarketDataCache}.
 *
 * <p>This provider uses a fixed {@link TimeInterval} configured at
 * construction time. All price lookups are delegated to the underlying
 * cache using that interval.
 *
 * <p>This is useful when a trading system or strategy operates entirely
 * on a single interval and should not need to repeatedly specify it.
 */
public class CachedPriceProvider implements PriceProvider {

    private final MarketDataCache marketDataCache;
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
            MarketDataCache marketDataCache,
            TimeInterval interval
    ) {
        if (marketDataCache == null) {
            throw new IllegalArgumentException(
                    "MarketDataCache cannot be null."
            );
        }

        if (interval == null) {
            throw new IllegalArgumentException(
                    "TimeInterval cannot be null."
            );
        }

        this.marketDataCache = marketDataCache;
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
    ) {
        return marketDataCache.getTickerPrice(
                ticker,
                interval,
                timestamp
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