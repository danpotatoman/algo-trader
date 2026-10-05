package com.algotrader.decision.dataobjects;

import java.util.List;

import com.algotrader.registry.OpenTradeAdjustment;

/**
 * Result of evaluating portfolio decisions for a single trading cycle.
 *
 * <p>A {@code PortfolioDecisionResult} contains the list of new trades
 * to be opened and existing trades to be adjusted.
 *
 * @param newCapitalAllocations new trade intent
 * @param openTradeAdjustments updates to apply to the open trade registry
 */
public record PortfolioDecisionResult(
        List<CapitalAllocation> newCapitalAllocations,
        List<OpenTradeAdjustment> openTradeAdjustments,
        List<SignalDecision> signals,
        long predictionDurationNanos,
        int inferenceBatchSize) {

    public PortfolioDecisionResult(List<CapitalAllocation> allocations, List<OpenTradeAdjustment> adjustments) {
        this(allocations, adjustments, List.of(), 0, 0);
    }

    /**
     * Creates an immutable portfolio decision result.
     *
     * <p>The supplied lists are defensively copied to prevent subsequent
     * modification by callers.
     */
    public PortfolioDecisionResult {
        newCapitalAllocations = List.copyOf(newCapitalAllocations);
        openTradeAdjustments = List.copyOf(openTradeAdjustments);
        signals = List.copyOf(signals);
    }
}
