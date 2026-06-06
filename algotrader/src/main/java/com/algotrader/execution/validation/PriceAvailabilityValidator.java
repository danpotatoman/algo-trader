package com.algotrader.execution.validation;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;

/**
 * Temporary validator for checking whether prices exist for both legs of a
 * {@link RoundTripTrade}.
 *
 * <p>This class currently uses {@link PriceProvider#getTickerPrice(String,
 * java.time.Instant)} as an existence check by attempting to retrieve prices
 * for the trade entry and exit timestamps.
 *
 * <p><b>TODO:</b> Remove or redesign this class. Price availability should
 * likely be handled by the market data layer itself, either through a
 * non-throwing availability method or a clearer {@code PriceProvider}
 * contract. Using exceptions for ordinary missing-data checks may not be the
 * right abstraction.
 */
public class PriceAvailabilityValidator {

    private final PriceProvider priceProvider;

    public PriceAvailabilityValidator(PriceProvider priceProvider) {
        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "PriceProvider cannot be null."
            );
        }

        this.priceProvider = priceProvider;
    }

    public boolean pricesExist(RoundTripTrade trade) {

    try {
        priceProvider.getTickerPrice(
                trade.ticker(),
                trade.entryTime()
        );

        priceProvider.getTickerPrice(
                trade.ticker(),
                trade.exitTime()
        );

        return true;

    } catch (DataCacheException e) {
        return false;
    }
}

// TODO: This method is currently ineffective because pricesExist catches
//       DataCacheException. Remove this class or replace it after the
//       PriceProvider missing-data contract is redesigned.
    public void validatePricesExist(RoundTripTrade trade)
            throws DataCacheException {

        pricesExist(trade);
    }
}