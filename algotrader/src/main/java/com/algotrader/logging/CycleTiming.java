package com.algotrader.logging;

/** Cycle work through valuation; artifact serialization is measured separately at run level. */
public record CycleTiming(long totalNanos, long exitNanos, long evaluationNanos,
                          long entryAndAdjustmentNanos, long valuationNanos) {}
