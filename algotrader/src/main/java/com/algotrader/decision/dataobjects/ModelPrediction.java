package com.algotrader.decision.dataobjects;

import java.time.Instant;

import com.algotrader.marketdata.model.TimeInterval;

public interface ModelPrediction {
    String getTicker();
    Instant getFinalTimestamp();
    TimeInterval getInterval();
    String summary();
}