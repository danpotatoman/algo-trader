package com.algotrader.decision.generator;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.PortfolioDecisionResult;
import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.portfolio.PortfolioSnapshot;

/**
 * Generates portfolio-level trading decisions from market data and the
 * current portfolio state.
 *
 * <p>A {@code PortfolioDecisionGenerator} is responsible for converting
 * collections of {@link DataBatch} instances into a
 * {@link PortfolioDecisionResult} describing the actions and registry updates
 * for the current trading cycle.
 *
 * <p>Implementations will typically obtain model forecasts for each supplied
 * {@link DataBatch} (for example by querying a Python-hosted machine learning
 * model), evaluate those forecasts in the context of the current
 * {@link PortfolioSnapshot}, and determine an appropriate set of portfolio
 * actions such as opening new positions, closing existing positions,
 * extending planned holding periods, or taking no action.
 *
 * <p>The supplied {@link PortfolioSnapshot} represents the portfolio state at
 * the beginning of the trading cycle and is treated as read-only. Applying
 * the resulting {@link PortfolioDecisionResult} and updating portfolio state
 * are the responsibility of higher-level runtime components.
 */
public interface PortfolioDecisionGenerator {

    default com.fasterxml.jackson.databind.JsonNode provenance() throws Exception {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    }

    /**
     * Generates a portfolio decision for the current trading cycle.
     *
     * @param batches complete market data windows for one or more tickers
     * @param portfolio immutable snapshot of the portfolio at the start of
     *                  the trading cycle
     * @return the portfolio actions that should be taken for this cycle
     */
    PortfolioDecisionResult generateDecision(
            List<DataBatch> batches,
            PortfolioSnapshot portfolio,
            List<RoundTripTrade> openTrades,
            Instant cycleTime
    ) throws DecisionGenerationException;
}
