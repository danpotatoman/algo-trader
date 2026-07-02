package com.algotrader.logging;

import java.time.Instant;
import java.util.List;

import com.algotrader.decision.dataobjects.CapitalAllocation;
import com.algotrader.portfolio.PortfolioSnapshot;
import com.algotrader.trade.registry.OpenTradeAdjustment;

/**
 * Result of evaluating a single multi-ticker trading cycle.
 *
 * <p>A {@code MultiTickerCycleEvaluation} captures the outcome of evaluating
 * the market and current portfolio state at a specific cycle time. It
 * contains the portfolio snapshot used during evaluation together with new
 * capital allocations and open-trade registry updates for the caller to apply.
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
     * <p>The supplied allocation and registry update lists are defensively
     * copied.
     */
    public MultiTickerCycleEvaluation {
        newCapitalAllocations = List.copyOf(newCapitalAllocations);
        openTradeAdjustments = List.copyOf(openTradeAdjustments);
    }
}
