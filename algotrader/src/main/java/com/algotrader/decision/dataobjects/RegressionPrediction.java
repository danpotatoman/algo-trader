package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;


public final class RegressionPrediction implements ModelPrediction {

    private final DataBatch dataBatch;
    private final RegressionForecast forecast;

    public RegressionPrediction(
            DataBatch dataBatch,
            RegressionForecast forecast
    ) {

        if (dataBatch == null) {
            throw new IllegalArgumentException(
                    "DataBatch cannot be null."
            );
        }

        if (forecast == null) {
            throw new IllegalArgumentException(
                    "RegressionForecast cannot be null."
            );
        }

        this.dataBatch = dataBatch;
        this.forecast = forecast;
    }

    @Override
    public String getTicker() {
        return dataBatch.getTicker();
    }

    @Override
    public Instant getFinalTimestamp() {
        return dataBatch.getFinalTimestamp();
    }

    @Override
    public TimeInterval getInterval() {
        return dataBatch.getInterval();
    }

    @Override
    public String summary() {
        return String.format(
                "RegressionPrediction[ticker=%s, 5m=%.4f, 10m=%.4f, 30m=%.4f, vol30m=%.4f]",
                getTicker(),
                forecast.getReturn5m(),
                forecast.getReturn10m(),
                forecast.getReturn30m(),
                forecast.getVolatility30m()
        );
    }
}