package com.algotrader.data.dataobjects;

import java.time.Instant;

import com.algotrader.data.TimeInterval;

public interface ModelPrediction {
    String getTicker();
    Instant getFinalTimestamp();
    TimeInterval getInterval();
    String summary();
}