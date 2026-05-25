package com.algotrader.data.dataobjects;

public class RegressionForecast {

    private final double return5m;
    private final double return10m;
    private final double return30m;
    private final double volatility30m;

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

    /**
     * Returns the predicted return over the next 5 minutes.
     *
     * @return predicted 5-minute return
     */
    public double getReturn5m() {
        return return5m;
    }

    /**
     * Returns the predicted return over the next 10 minutes.
     *
     * @return predicted 10-minute return
     */
    public double getReturn10m() {
        return return10m;
    }

    /**
     * Returns the predicted return over the next 30 minutes.
     *
     * @return predicted 30-minute return
     */
    public double getReturn30m() {
        return return30m;
    }

    /**
     * Returns the predicted 30-minute volatility.
     *
     * @return predicted 30-minute volatility
     */
    public double getVolatility30m() {
        return volatility30m;
    }
}