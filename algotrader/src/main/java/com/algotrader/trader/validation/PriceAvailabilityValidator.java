package com.algotrader.trader.validation;

import com.algotrader.data.cache.DataCacheException;
import com.algotrader.data.provider.PriceProvider;
import com.algotrader.strategy.RoundTripTrade;

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

    public void validatePricesExist(RoundTripTrade trade)
            throws DataCacheException {

        pricesExist(trade);
    }
}