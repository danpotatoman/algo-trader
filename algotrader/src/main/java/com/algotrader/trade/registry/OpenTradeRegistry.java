package com.algotrader.trade.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Registry of open round-trip trades.
 *
 * <p>The registry tracks planned trades that have entered but have not yet
 * completed their planned exit. It does not execute trades or mutate
 * portfolio state directly. Instead, decision-generation components may
 * inspect the registry and produce {@code PortfolioAction}s when open trades
 * become due for exit.
 */
public final class OpenTradeRegistry implements OpenTradeRegistryView {

    private final List<RoundTripTrade> openTrades;

    public OpenTradeRegistry() {
        this.openTrades = new ArrayList<>();
    }

    /**
     * Returns all currently open trades.
     *
     * @return immutable copy of open trades
     */
    public synchronized List<RoundTripTrade> getOpenTrades() {
        return List.copyOf(openTrades);
    }

    /**
     * Returns open trades whose planned exit time is at or before the supplied
     * timestamp.
     *
     * @param timestamp timestamp to compare against planned exit times
     * @return immutable list of trades due for exit
     */
    public synchronized List<RoundTripTrade> getTradesDueForExit(
            Instant timestamp) {

        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        return openTrades.stream()
                .filter(trade -> !trade.exitTime().isAfter(timestamp))
                .toList();
    }
}