package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.PortfolioAction;
import com.algotrader.portfolio.PortfolioSnapshot;
import com.algotrader.runtime.OpenTradeRegistryUpdate;

/**
 * Result of evaluating a single multi-ticker trading cycle.
 *
 * <p>A {@code MultiTickerCycleEvaluation} captures the outcome of evaluating
 * the market and current portfolio state at a specific cycle time. It
 * contains the portfolio snapshot used during evaluation together with the
 * portfolio actions and open-trade registry updates that should be applied
 * by the caller.
 *
 * <p>This class does not imply that any actions have been executed or applied.
 *
 * @param cycleTime trading cycle timestamp
 * @param cycleDurationMillis evaluation duration in milliseconds
 * @param portfolioSnapshot portfolio state used during evaluation
 * @param actions portfolio actions generated for this cycle
 * @param registryUpdates updates to apply to the open trade registry
 */
public record MultiTickerCycleEvaluation(
        Instant cycleTime,
        long cycleDurationMillis,
        PortfolioSnapshot portfolioSnapshot,
        List<PortfolioAction> actions,
        List<OpenTradeRegistryUpdate> registryUpdates) {

    /**
     * Creates an immutable trading cycle evaluation.
     *
     * <p>The supplied action and registry update lists are defensively copied.
     */
    public MultiTickerCycleEvaluation {
        actions = List.copyOf(actions);
        registryUpdates = List.copyOf(registryUpdates);
    }
}