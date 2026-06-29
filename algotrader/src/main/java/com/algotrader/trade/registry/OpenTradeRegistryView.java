package com.algotrader.trade.registry;

import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Read-only view of currently open trades.
 */
public interface OpenTradeRegistryView {

    /**
     * Returns the currently open round-trip trades.
     *
     * @return open trades
     */
    List<RoundTripTrade> getOpenTrades();
}