package com.algotrader.decision.generator;

import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.prediction.provider.PredictionProviderException;
import com.algotrader.marketdata.model.DataBatch;

/**
 * High-level abstraction for generating trades from market data.
 *
 * <p>A {@code TradeGenerator} encapsulates the model-dependent decision
 * pipeline:
 *
 * <pre>
 * DataBatch -> ModelPrediction -> List&lt;RoundTripTrade&gt;
 * </pre>
 *
 * <p>Implementations typically:
 * <ol>
 *     <li>Request a model prediction for the supplied market data</li>
 *     <li>Plan trades from that prediction according to strategy rules</li>
 *     <li>Return valid round-trip trades, or an empty list if no trade
 *         opportunities are identified</li>
 * </ol>
 *
 * <p>Different implementations may use different prediction providers,
 * prediction types, trade planners, or validation rules.
 *
 * <p>The trading service should depend on this abstraction rather than
 * directly coordinating prediction providers and trade planners itself.
 */
public interface TradeGenerator {

    /**
     * Generates round-trip trades from the supplied market data batch.
     *
     * <p>The returned trades represent complete entry and exit plans.
     * Implementations may return an empty list if no valid trade opportunities
     * are identified.
     *
     * @param batch market data batch used as model input
     * @return generated round-trip trades, possibly empty
     * @throws IllegalArgumentException if {@code batch} is null or invalid
     * @throws PredictionProviderException if prediction generation fails
     */
    List<RoundTripTrade> generateTrades(
            DataBatch batch
    ) throws PredictionProviderException;
}
