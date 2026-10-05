package com.algotrader.portfolio;

import java.time.Instant;
import java.util.Map;

/** A marked portfolio; unknown prices make total equity unknown, never zero. */
public record PortfolioValuation(
        Instant timestamp, String samplingPoint, double cash,
        Map<String, PositionValue> positions, Double totalEquity, String status) {
    public PortfolioValuation {
        positions = Map.copyOf(positions);
    }

    public record PositionValue(double quantity, Double price, Instant priceTimestamp,
                                Double value, String status) {}
}
