package com.algotrader.decision.dataobjects;

/** Compact prediction and the reason for its disposition; fills record the applied allocation. */
public record SignalDecision(String ticker, double probability, double volatility,
                             int horizonMinutes, Double score, String reason,
                             double requestedCash) {}
