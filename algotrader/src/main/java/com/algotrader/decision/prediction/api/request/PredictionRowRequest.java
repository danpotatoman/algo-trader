package com.algotrader.decision.prediction.api.request;

import com.algotrader.marketdata.model.StampedOHLCV;

/**
 * OHLCV row payload sent to row-oriented prediction endpoints.
 */
public record PredictionRowRequest(
        /** Candle timestamp in ISO-8601 format. */
        String timestamp,

        /** Opening price. */
        double open,

        /** Highest price. */
        double high,

        /** Lowest price. */
        double low,

        /** Closing price. */
        double close,

        /** Traded volume. */
        double volume
) {

    /**
     * Creates a request row from a stamped OHLCV market data row.
     *
     * @param row source market data row
     * @return prediction request row
     */
    public static PredictionRowRequest from(StampedOHLCV row) {
        return new PredictionRowRequest(
                row.candleOpenTime().toString(),
                row.open(),
                row.high(),
                row.low(),
                row.close(),
                row.volume()
        );
    }
}
