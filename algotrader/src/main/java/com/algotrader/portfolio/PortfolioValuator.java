package com.algotrader.portfolio;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.algotrader.execution.TradeExecutionResult;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.MarketPrice;
import com.algotrader.marketdata.provider.PriceProvider;

/** Marks holdings at the cycle's opening price, explicitly carrying prior marks across gaps. */
public final class PortfolioValuator {
    private final PriceProvider prices;
    private final Map<String, MarketPrice> lastPrices = new HashMap<>();

    public PortfolioValuator(PriceProvider prices) {
        this.prices = java.util.Objects.requireNonNull(prices);
    }

    public void observe(TradeExecutionResult fill) {
        lastPrices.put(fill.ticker(), new MarketPrice(fill.ticker(), fill.price(), fill.executionTime()));
    }

    public PortfolioValuation value(PortfolioSnapshot snapshot, Instant time, String samplingPoint) {
        Map<String, PortfolioValuation.PositionValue> positions = new java.util.TreeMap<>();
        double equity = snapshot.cash();
        boolean missing = false;
        boolean stale = false;
        for (var position : snapshot.positions().entrySet()) {
            if (position.getValue() == 0.0) continue;
            String ticker = position.getKey();
            try {
                MarketPrice price = prices.getTickerPrice(ticker, time);
                if (Double.isFinite(price.price()) && price.price() > 0
                        && !price.executionTime().isAfter(time)) {
                    lastPrices.put(ticker, price);
                }
            } catch (DataCacheException e) {
                // Missing candles are represented by a stale or missing mark below.
            }
            MarketPrice price = lastPrices.get(ticker);
            if (price == null || price.executionTime().isAfter(time)
                    || !Double.isFinite(price.price()) || price.price() <= 0) {
                missing = true;
                positions.put(ticker, new PortfolioValuation.PositionValue(
                        position.getValue(), null, null, null, "MISSING"));
            } else {
                boolean current = price.executionTime().equals(time);
                stale |= !current;
                double value = price.price() * position.getValue();
                equity += value;
                positions.put(ticker, new PortfolioValuation.PositionValue(
                        position.getValue(), price.price(), price.executionTime(), value,
                        current ? "CURRENT" : "STALE"));
            }
        }
        return new PortfolioValuation(time, samplingPoint, snapshot.cash(), positions,
                missing ? null : equity, missing ? "INCOMPLETE" : stale ? "STALE" : "COMPLETE");
    }
}
