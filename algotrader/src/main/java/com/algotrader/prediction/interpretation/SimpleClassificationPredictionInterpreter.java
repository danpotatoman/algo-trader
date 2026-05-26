package com.algotrader.prediction.interpretation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.TradeRecommendation;

public class SimpleClassificationPredictionInterpreter
        implements PredictionInterpreter<ClassificationPrediction> {

    private final double k;
    private final int horizonMinutes;

    public SimpleClassificationPredictionInterpreter(
            double k,
            int horizonMinutes
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

        this.k = k;
        this.horizonMinutes = horizonMinutes;
    }

    @Override
    public List<TradeRecommendation> getRecommendations(
            ClassificationPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "ClassificationPrediction cannot be null."
            );
        }

        String ticker = prediction.getTicker();

        Instant buyTime = prediction.getFinalTimestamp();

        if (buyTime == null) {
            throw new IllegalArgumentException(
                    "Prediction final timestamp cannot be null."
            );
        }

        Instant sellTime = buyTime.plus(
                horizonMinutes,
                ChronoUnit.MINUTES
        );

        double quantity =
                prediction.predictsUp()
                && prediction.getConfidence() >= k
                        ? 1.0
                        : 0.0;

        TradeRecommendation buy = new TradeRecommendation(
                ticker,
                TradeRecommendation.Action.BUY,
                prediction.getConfidence(),
                quantity,
                buyTime
        );

        TradeRecommendation sell = new TradeRecommendation(
                ticker,
                TradeRecommendation.Action.SELL,
                prediction.getConfidence(),
                quantity,
                sellTime
        );

        return List.of(buy, sell);
    }
}