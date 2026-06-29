package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.CapitalAllocation;
import com.algotrader.decision.dataobjects.OpenTradeAdjustment;
import com.algotrader.portfolio.PortfolioSnapshot;

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
 * @param newCapitalAllocations capital to allocate to new trades this cycle
 * @param openTradeAdjustments updates to exit times of trades in the open trade registry
 */
public record MultiTickerCycleEvaluation(
        Instant cycleTime,
        long cycleDurationMillis,
        PortfolioSnapshot portfolioSnapshot,
        List<CapitalAllocation> newCapitalAllocations,
        List<OpenTradeAdjustment> openTradeAdjustments) {

    /**
     * Creates an immutable trading cycle evaluation.
     *
     * <p>The supplied action and registry update lists are defensively copied.
     */
    public MultiTickerCycleEvaluation {
        newCapitalAllocations = List.copyOf(newCapitalAllocations);
        openTradeAdjustments = List.copyOf(openTradeAdjustments);
    }
}