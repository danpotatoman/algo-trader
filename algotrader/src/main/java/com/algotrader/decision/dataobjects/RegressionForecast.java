package com.algotrader.decision.dataobjects;

/**
 * Immutable regression forecast produced by a prediction model.
 *
 * <p>A {@code RegressionForecast} contains continuous-valued predictions
 * describing expected future market behavior over multiple forecast horizons.
 *
 * <p>Returns are expressed as fractional price changes rather than
 * percentages. For example, a value of {@code 0.01} represents an expected
 * return of 1%.
 *
 * <p>The forecast currently includes expected returns over 5, 10, and
 * 30-minute horizons, as well as predicted volatility over the next
 * 30 minutes.
 *
 * <p><b>Note:</b> The forecast structure currently reflects the output of the
 * project's existing regression model. Future models may require additional
 * forecast fields or a more flexible representation.
 */
public final class RegressionForecast {

    private final double return5m;
    private final double return10m;
    private final double return30m;
    private final double volatility30m;

    /**
     * Creates a regression forecast.
     *
     * @param return5m predicted return over the next 5 minutes
     * @param return10m predicted return over the next 10 minutes
     * @param return30m predicted return over the next 30 minutes
     * @param volatility30m predicted volatility over the next 30 minutes
     */
    public RegressionForecast(
            double return5m,
            double return10m,
            double return30m,
            double volatility30m
    ) {
        this.return5m = return5m;
        this.return10m = return10m;
        this.return30m = return30m;
        this.volatility30m = volatility30m;
    }

    public double getReturn5m() {
        return return5m;
    }

    public double getReturn10m() {
        return return10m;
    }

    public double getReturn30m() {
        return return30m;
    }

    public double getVolatility30m() {
        return volatility30m;
    }
}