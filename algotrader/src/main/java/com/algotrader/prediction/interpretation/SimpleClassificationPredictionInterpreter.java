package com.algotrader.prediction.interpretation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.strategy.RoundTripTrade;

public class SimpleClassificationPredictionInterpreter
        implements PredictionInterpreter<ClassificationPrediction> {

    private final double k;
    private final int horizonMinutes;
    private final String strategyId;

    public SimpleClassificationPredictionInterpreter(
            double k,
            int horizonMinutes,
            String strategyId
    ) {
        if (k < 0.0 || k > 1.0) {
            throw new IllegalArgumentException(
                    "Confidence threshold k must be between 0 and 1."
            );
        }

        if (horizonMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Horizon minutes must be positive."
            );
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Strategy ID cannot be null or blank."
            );
        }

        this.k = k;
        this.horizonMinutes = horizonMinutes;
        this.strategyId = strategyId;
    }

    @Override
    public List<RoundTripTrade> getTrades(
            ClassificationPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "ClassificationPrediction cannot be null."
            );
        }

        if (!prediction.predictsUp()
                || prediction.getConfidence() < k) {
            return List.of();
        }

        String ticker = prediction.getTicker();

        Instant entryTime = prediction.getFinalTimestamp();

        if (entryTime == null) {
            throw new IllegalArgumentException(
                    "Prediction final timestamp cannot be null."
            );
        }

        Instant exitTime = entryTime.plus(
                horizonMinutes,
                ChronoUnit.MINUTES
        );

        RoundTripTrade trade = new RoundTripTrade(
                ticker,
                1,
                entryTime,
                exitTime,
                prediction.getConfidence(),
                strategyId
        );

        return List.of(trade);
    }
}