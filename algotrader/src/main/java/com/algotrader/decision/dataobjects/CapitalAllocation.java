package com.algotrader.decision.dataobjects;

import java.time.Instant;

public record CapitalAllocation(
        String ticker,
        double cashAmount,
        Instant entryTime,
        Instant plannedExitTime,
        String strategyId
) {
}