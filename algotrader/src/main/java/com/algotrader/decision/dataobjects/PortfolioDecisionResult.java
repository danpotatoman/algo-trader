package com.algotrader.decision.dataobjects;

import java.util.List;

import com.algotrader.runtime.OpenTradeRegistryUpdate;

/**
 * Result of evaluating portfolio decisions for a single trading cycle.
 *
 * <p>A {@code PortfolioDecisionResult} contains the portfolio actions that
 * should be applied immediately together with any updates that should be made
 * to the open trade registry.
 *
 * @param actions portfolio actions to apply for the current cycle
 * @param registryUpdates updates to apply to the open trade registry
 */
public record PortfolioDecisionResult(
        List<PortfolioAction> actions,
        List<OpenTradeRegistryUpdate> registryUpdates) {

    /**
     * Creates an immutable portfolio decision result.
     *
     * <p>The supplied lists are defensively copied to prevent subsequent
     * modification by callers.
     */
    public PortfolioDecisionResult {
        actions = List.copyOf(actions);
        registryUpdates = List.copyOf(registryUpdates);
    }
}