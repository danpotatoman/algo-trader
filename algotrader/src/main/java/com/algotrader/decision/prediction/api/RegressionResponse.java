package com.algotrader.decision.prediction.api;

/**
 * Response payload returned by a regression prediction endpoint.
 *
 * <p>This record represents the JSON response returned by the Python model
 * service and serves as a transport object between the HTTP API layer and
 * the application's regression prediction domain objects.
 *
 * <p>Instances are typically deserialized directly from endpoint responses
 * before being converted into {@code RegressionForecast} objects.
 *
 * <p>The current response format contains predicted returns over multiple
 * forecast horizons along with a predicted 30-minute volatility value.
 *
 * <p><b>Note:</b> This DTO reflects the output schema of the project's
 * current regression model. Future models may produce different forecast
 * horizons, additional prediction targets, or a more flexible output
 * structure, in which case this record may need to evolve.
 *
 * <p>Expected JSON format:
 *
 * <pre>
 * {
 *   "ticker": "AAPL",
 *   "return5m": 0.0012,
 *   "return10m": 0.0027,
 *   "return30m": 0.0041,
 *   "volatility30m": 0.0065
 * }
 * </pre>
 */
public record RegressionResponse(

        /** Ticker symbol associated with the prediction. */
        String ticker,

        /**
         * Predicted return over the next 5 minutes.
         *
         * <p>Returns are expressed as fractional price changes rather than
         * percentages. For example, {@code 0.01} represents a predicted return
         * of 1%.
         */
        double return5m,

        /** Predicted return over the next 10 minutes. */
        double return10m,

        /** Predicted return over the next 30 minutes. */
        double return30m,

        /** Predicted volatility over the next 30 minutes. */
        double volatility30m

) {}