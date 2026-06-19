// package com.algotrader.marketdata.provider;

// import java.time.Instant;

// import com.algotrader.marketdata.cache.DataCacheException;
// import com.algotrader.marketdata.model.MarketPrice;
// import com.algotrader.marketdata.model.StampedOHLCV;
// import com.algotrader.marketdata.model.TimeInterval;

// /**
//  * Legacy {@link PriceProvider} implementation backed by a
//  * {@link MarketDataProvider}.
//  *
//  * <p>This adapter was originally introduced before
//  * {@code MarketDataCache} implemented {@link PriceProvider} directly.
//  * It converts OHLCV row lookups into {@link MarketPrice} objects by
//  * retrieving a candle from a fixed {@link TimeInterval} and returning
//  * its close price.
//  *
//  * <p>The current architecture typically uses {@code MarketDataCache}
//  * directly as both a {@code MarketDataProvider} and
//  * {@code PriceProvider}, making this adapter largely unnecessary.
//  *
//  * <p><b>Legacy status:</b> Retained for compatibility with older code paths.
//  *
//  * <p><b>TODO:</b> Remove this class if no remaining components require
//  * interval-specific price lookup adaptation.
//  */
// public final class CachedPriceProvider implements PriceProvider {

//     private final MarketDataProvider marketDataProvider;
//     private final TimeInterval interval;

//     /**
//      * Creates a legacy price-provider adapter for a fixed interval.
//      *
//      * @param marketDataProvider backing provider used to retrieve OHLCV rows
//      * @param interval interval used for all price lookups
//      * @throws IllegalArgumentException if either argument is null
//      */
//     public CachedPriceProvider(
//             MarketDataProvider marketDataProvider,
//             TimeInterval interval
//     ) {
//         if (marketDataProvider == null) {
//             throw new IllegalArgumentException(
//                     "MarketDataCache cannot be null."
//             );
//         }

//         if (interval == null) {
//             throw new IllegalArgumentException(
//                     "TimeInterval cannot be null."
//             );
//         }

//         this.marketDataProvider = marketDataProvider;
//         this.interval = interval;
//     }

//     /**
//      * Retrieves the market price for a ticker at an exact timestamp.
//      *
//      * <p>The returned price is derived from the close value of the matching
//      * OHLCV candle retrieved from the backing provider.
//      *
//      * @param ticker ticker symbol to query
//      * @param timestamp exact timestamp to retrieve
//      * @return market price derived from the candle close
//      * @throws DataCacheException if the requested row cannot be retrieved
//      */
//     @Override
//     public MarketPrice getTickerPrice(
//             String ticker,
//             Instant timestamp
//     ) throws DataCacheException {
//         StampedOHLCV row = marketDataProvider.requestRow(
//                 ticker,
//                 interval,
//                 timestamp
//         );

//         return new MarketPrice(
//                 ticker.toUpperCase(),
//                 row.close(),
//                 row.timestamp()
//         );
//     }

//     /**
//      * Returns the fixed interval used by this provider.
//      *
//      * @return the configured interval
//      */
//     public TimeInterval getInterval() {
//         return interval;
//     }
// }