package com.algotrader.registry;

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
 * inspect the registry and request exit-time adjustments while the
 * orchestration layer handles exits that become due.
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

    /**
     * Closes an open trade by removing it from the registry.
     *
     * @param trade trade to close
     * @throws IllegalArgumentException if {@code trade} is null
     * @throws IllegalStateException if the trade is not currently open
     */
    public synchronized void close(RoundTripTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null.");
        }

        if (!openTrades.remove(trade)) {
            throw new IllegalStateException(
                    "Trade is not currently open: " + trade);
        }
    }

    /**
     * Closes each supplied trade by removing it from the registry.
     *
     * <p>Trades are closed in iteration order. If any trade is not currently
     * open, an exception is thrown and subsequent trades are not processed.
     *
     * @param trades trades to close
     * @throws IllegalArgumentException if {@code trades} is null or contains null
     * @throws IllegalStateException if any trade is not currently open
     */
    public synchronized void closeAll(List<RoundTripTrade> trades) {
        if (trades == null) {
            throw new IllegalArgumentException("Trades cannot be null.");
        }

        for (RoundTripTrade trade : trades) {
            close(trade);
        }
    }

    /**
     * Adds an open trade to the registry.
     *
     * @param trade trade to add
     * @throws IllegalArgumentException if {@code trade} is null
     * @throws IllegalStateException if the trade is already open
     */
    public synchronized void add(RoundTripTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null.");
        }

        if (openTrades.contains(trade)) {
            throw new IllegalStateException(
                    "Trade is already open: " + trade);
        }

        openTrades.add(trade);
    }

    /**
     * Adds each supplied trade to the registry.
     *
     * <p>Trades are added in iteration order. If any trade is already open,
     * an exception is thrown and subsequent trades are not processed.
     *
     * @param trades trades to add
     * @throws IllegalArgumentException if {@code trades} is null or contains null
     * @throws IllegalStateException if any trade is already open
     */
    public synchronized void addAll(List<RoundTripTrade> trades) {
        if (trades == null) {
            throw new IllegalArgumentException("Trades cannot be null.");
        }

        for (RoundTripTrade trade : trades) {
            add(trade);
        }
    }

    /**
     * Applies an adjustment to an open trade.
     *
     * <p>The target trade is replaced with an equivalent trade having the updated
     * planned exit time.
     *
     * @param adjustment adjustment to apply
     * @throws IllegalArgumentException if {@code adjustment} is null
     * @throws IllegalStateException if the target trade is not currently open
     */
    public synchronized void adjust(OpenTradeAdjustment adjustment) {
        if (adjustment == null) {
            throw new IllegalArgumentException(
                    "Adjustment cannot be null.");
        }

        RoundTripTrade targetTrade = adjustment.targetTrade();

        int index = openTrades.indexOf(targetTrade);

        if (index < 0) {
            throw new IllegalStateException(
                    "Trade is not currently open: " + targetTrade);
        }

        RoundTripTrade adjustedTrade = new RoundTripTrade(
                targetTrade.ticker(),
                targetTrade.quantity(),
                targetTrade.entryTime(),
                adjustment.newExitTime(),
                targetTrade.strategyId()
        );

        openTrades.set(index, adjustedTrade);
    }

    /**
     * Applies each supplied adjustment.
     *
     * <p>Adjustments are applied in iteration order. If any adjustment targets a
     * trade that is not currently open, an exception is thrown and subsequent
     * adjustments are not processed.
     *
     * @param adjustments adjustments to apply
     * @throws IllegalArgumentException if {@code adjustments} is null or contains
     *         null
     * @throws IllegalStateException if any target trade is not currently open
     */
    public synchronized void adjustAll(
            List<OpenTradeAdjustment> adjustments) {

        if (adjustments == null) {
            throw new IllegalArgumentException(
                    "Adjustments cannot be null.");
        }

        for (OpenTradeAdjustment adjustment : adjustments) {
            adjust(adjustment);
        }
    }
}
