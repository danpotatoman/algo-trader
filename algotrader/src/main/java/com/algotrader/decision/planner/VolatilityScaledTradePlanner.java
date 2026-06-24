package com.algotrader.decision.planner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.algotrader.account.PaperAccount;
import com.algotrader.decision.dataobjects.ClassificationWithVolatilityPrediction;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.MarketPrice;
import com.algotrader.marketdata.provider.PriceProvider;

/**
 * Plans classification trades whose position size decreases as predicted
 * volatility rises relative to endpoint output statistics.
 *
 * <p>The planner requires a minimum upward-move probability, maps forecast
 * volatility to a bounded position fraction, and converts the allocated paper
 * account cash to a fractional quantity using the entry-time market price.
 * No trade is produced when the confidence threshold is not met or an
 * entry-time price is unavailable.
 */
public final class VolatilityScaledTradePlanner
        implements TradePlanner<ClassificationWithVolatilityPrediction> {

    private static final int EXIT_DELAY_MINUTES = 30;

    private final PaperAccount paperAccount;
    private final PriceProvider priceProvider;
    private final String strategyId;
    private final double volatilityMean;
    private final double volatilityStd;
    private final double minConfidenceThreshold;
    private final double minPositionFraction;
    private final double maxPositionFraction;

    /**
     * Creates a volatility-scaled trade planner.
     *
     * @param paperAccount account supplying cash available for position sizing
     * @param priceProvider provider used to resolve the entry-time price
     * @param strategyId identifier associated with generated trades
     * @param volatilityMean mean volatility used to standardize predictions
     * @param volatilityStd positive volatility standard deviation
     * @param minConfidenceThreshold minimum upward probability required
     * @param minPositionFraction minimum fraction of cash allocated to a trade
     * @param maxPositionFraction maximum fraction of cash allocated to a trade
     * @throws IllegalArgumentException if any dependency or configuration value
     *         is invalid
     */
    public VolatilityScaledTradePlanner(
            PaperAccount paperAccount,
            PriceProvider priceProvider,
            String strategyId,
            double volatilityMean,
            double volatilityStd,
            double minConfidenceThreshold,
            double minPositionFraction,
            double maxPositionFraction
    ) {
        if (paperAccount == null) {
            throw new IllegalArgumentException(
                    "Paper account cannot be null."
            );
        }

        if (priceProvider == null) {
            throw new IllegalArgumentException(
                    "Price provider cannot be null."
            );
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        if (volatilityStd <= 0.0) {
            throw new IllegalArgumentException(
                    "Volatility standard deviation must be positive."
            );
        }

        if (minConfidenceThreshold < 0.0 || minConfidenceThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "Minimum confidence threshold must be between 0 and 1."
            );
        }

        if (minPositionFraction < 0.0 || minPositionFraction > 1.0) {
            throw new IllegalArgumentException(
                    "Minimum position fraction must be between 0 and 1."
            );
        }

        if (maxPositionFraction < 0.0 || maxPositionFraction > 1.0) {
            throw new IllegalArgumentException(
                    "Maximum position fraction must be between 0 and 1."
            );
        }

        if (minPositionFraction > maxPositionFraction) {
            throw new IllegalArgumentException(
                    "Minimum position fraction cannot exceed maximum position fraction."
            );
        }

        this.paperAccount = paperAccount;
        this.priceProvider = priceProvider;
        this.strategyId = strategyId;
        this.volatilityMean = volatilityMean;
        this.volatilityStd = volatilityStd;
        this.minConfidenceThreshold = minConfidenceThreshold;
        this.minPositionFraction = minPositionFraction;
        this.maxPositionFraction = maxPositionFraction;
    }

    /**
     * Generates a volatility-scaled round-trip trade when the prediction meets
     * the confidence threshold and an entry price is available.
     *
     * @param prediction classification prediction with forecast volatility
     * @return a single sized trade, or an empty list when no trade can be
     *         planned
     * @throws IllegalArgumentException if {@code prediction} is null
     * @throws IllegalStateException if the resolved market price is not
     *         positive
     */
    @Override
    public List<RoundTripTrade> getTrades(
            ClassificationWithVolatilityPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "Prediction cannot be null."
            );
        }

        if (prediction.getProbability() < minConfidenceThreshold) {
            return List.of();
        }

        String ticker = prediction.getTicker();
        Instant entryTime = prediction.getLastCandleCloseTimestamp();
        Instant exitTime = entryTime.plus(EXIT_DELAY_MINUTES, ChronoUnit.MINUTES);

        double zScore =
                (prediction.getVolatility() - volatilityMean) / volatilityStd;

        double riskScore = clamp(
                0.5 - 0.15 * zScore,
                0.0,
                1.0
        );

        double positionFraction =
                minPositionFraction
                        + (maxPositionFraction - minPositionFraction)
                        * riskScore;

        double tradeCash = paperAccount.getCash() * positionFraction;

        MarketPrice marketPrice;

        try {
            marketPrice = priceProvider.getTickerPrice(
                    ticker,
                    entryTime
            );

        } catch (DataCacheException e) {
            return List.of(); //skip trade if price cannot be found
        }

        double price = marketPrice.price();

        if (price <= 0.0) {
            throw new IllegalStateException(
                    "Market price must be positive."
            );
        }

        double quantity = tradeCash / price;

        if (quantity <= 0.0) {
            return List.of();
        }

        RoundTripTrade trade = new RoundTripTrade(
                ticker,
                quantity,
                entryTime,
                exitTime,
                strategyId
        );

        return List.of(trade);
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(min, Math.min(max, value));
    }

    public PaperAccount getPaperAccount() {
        return paperAccount;
    }

    public PriceProvider getPriceProvider() {
        return priceProvider;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public double getVolatilityMean() {
        return volatilityMean;
    }

    public double getVolatilityStd() {
        return volatilityStd;
    }

    public double getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }

    public double getMinPositionFraction() {
        return minPositionFraction;
    }

    public double getMaxPositionFraction() {
        return maxPositionFraction;
    }
}
