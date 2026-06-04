package com.algotrader.execution.validation;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.provider.PriceProvider;

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