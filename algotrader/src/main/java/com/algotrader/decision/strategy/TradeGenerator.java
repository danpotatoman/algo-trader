package com.algotrader.decision.strategy;

import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.marketdata.model.DataBatch;

/**
 * High-level trading strategy abstraction.
 *
 * <p>A {@code TradeGenerator} encapsulates the process of:
 * <ol>
 *     <li>Generating a model prediction from market data</li>
 *     <li>Interpreting that prediction into executable round-trip trades</li>
 * </ol>
 *
 * <p>This interface represents the model-dependent portion of the
 * trading pipeline:
 *
 * <pre>
 * DataBatch -> Prediction -> List&lt;RoundTripTrade&gt;
 * </pre>
 *
 * <p>Different implementations may use:
 * <ul>
 *     <li>Classification or regression models</li>
 *     <li>Different prediction providers</li>
 *     <li>Different interpretation strategies</li>
 * </ul>
 *
 * <p>The trading service should depend on this abstraction rather
 * than directly coordinating providers and interpreters itself.
 */
public interface TradeGenerator {

    /**
     * Generates round-trip trades from the supplied market data batch.
     *
     * <p>The returned trades represent complete entry and exit plans.
     * Implementations may return an empty list if no trade opportunities
     * are identified.
     *
     * @param batch the market data batch used for prediction
     * @return generated round-trip trades (possibly empty)
     *
     * @throws IllegalArgumentException if batch is null
     * @throws PredictionProviderException if prediction generation fails
     */
    List<RoundTripTrade> generateTrades(
            DataBatch batch
    ) throws PredictionProviderException;
}