package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.model.TimeInterval;

/**
 * Immutable regression prediction produced by a model for a specific
 * market data batch.
 *
 * <p>A {@code RegressionPrediction} combines:
 * <ul>
 *     <li>The {@link DataBatch} used as model input</li>
 *     <li>The resulting {@link RegressionForecast} produced by the model</li>
 * </ul>
 *
 * <p>Unlike classification predictions, which predict a discrete outcome,
 * regression predictions provide continuous-valued forecasts describing
 * expected future market behavior over one or more forecast horizons.
 *
 * <p>This class is intended to serve as the regression counterpart to
 * {@link ClassificationPrediction}, providing a common representation for
 * regression model outputs while preserving the market data context from
 * which those outputs were generated.
 *
 * <p><b>TODO:</b> This class is currently incomplete. It should eventually
 * expose access to its underlying {@link RegressionForecast} and
 * {@link DataBatch}, and may provide additional convenience methods for
 * accessing forecast values in a manner consistent with
 * {@link ClassificationPrediction}.
 */
public final class RegressionPrediction implements ModelPrediction {

    private final DataBatch dataBatch;
    private final RegressionForecast forecast;
// TODO: Add accessors for DataBatch and RegressionForecast.
//       Align public API with ClassificationPrediction.
    /**
     * Creates a regression prediction from a model input batch and forecast.
     *
     * @param dataBatch the market data batch used as model input
     * @param forecast the regression forecast produced by the model
     * @throws IllegalArgumentException if either argument is null
     */
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