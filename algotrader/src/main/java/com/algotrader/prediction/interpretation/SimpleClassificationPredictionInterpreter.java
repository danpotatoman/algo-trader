package com.algotrader.prediction.interpretation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.algotrader.data.dataobjects.ClassificationPrediction;
import com.algotrader.data.dataobjects.TradeRecommendation;

public class SimpleClassificationPredictionInterpreter
        implements PredictionInterpreter<ClassificationPrediction> {

    private final double k;

    public SimpleClassificationPredictionInterpreter(double k) {
        if (k < 0.0 || k > 1.0) {
            throw new IllegalArgumentException(
                    "Confidence threshold k must be between 0 and 1."
            );
        }

        this.k = k;
    }

    @Override
    public TradeRecommendation[] getRecommendations(
            ClassificationPrediction prediction
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException(
                    "ClassificationPrediction cannot be null."
            );
        }

        if (!prediction.predictsUp() || prediction.getConfidence() < k) {
            return new TradeRecommendation[0];
        }

        String ticker = prediction.getTicker();
        Instant buyTime = prediction.getFinalTimestamp();

        if (buyTime == null) {
            return new TradeRecommendation[0];
        }

        Instant sellTime = buyTime.plus(10, ChronoUnit.MINUTES);

        TradeRecommendation buy = new TradeRecommendation(
                ticker,
                TradeRecommendation.Action.BUY,
                prediction.getConfidence(),
                1.0,
                buyTime
        );

        TradeRecommendation sell = new TradeRecommendation(
                ticker,
                TradeRecommendation.Action.SELL,
                prediction.getConfidence(),
                1.0,
                sellTime
        );

        return new TradeRecommendation[] { buy, sell };
    }
}