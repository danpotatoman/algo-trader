package com.algotrader.registry;

import java.time.Instant;
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

    /**
     * Returns open trades whose planned exit time is at or before the supplied
     * timestamp.
     *
     * @param timestamp timestamp to compare against planned exit times
     * @return immutable list of trades due for exit
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
    List<RoundTripTrade> getTradesDueForExit(Instant timestamp);

    /**
     * @return true if any round-trip trades are open, false if none are open
     */
    boolean hasOpenTrades();
}