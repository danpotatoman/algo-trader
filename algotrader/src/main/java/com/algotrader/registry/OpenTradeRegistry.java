package com.algotrader.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.generator.TradeExitTimePolicy;

/**
 * Registry of open round-trip trades.
 *
 * <p>The registry tracks planned trades that have entered but have not yet
 * completed their planned exit. It does not execute trades or mutate
 * portfolio state directly.
 *
 * <p>All newly opened and adjusted trades are validated against the configured
 * {@link TradeExitTimePolicy} before being stored.
 */
public final class OpenTradeRegistry implements OpenTradeRegistryView {

    private final List<RoundTripTrade> openTrades;
    private final TradeExitTimePolicy tradeExitTimePolicy;

    /**
     * Creates an open-trade registry.
     *
     * @param tradeExitTimePolicy policy used to validate planned exit times
     * @throws IllegalArgumentException if {@code tradeExitTimePolicy} is null
     */
    public OpenTradeRegistry(
            TradeExitTimePolicy tradeExitTimePolicy
    ) {
        if (tradeExitTimePolicy == null) {
            throw new IllegalArgumentException(
                    "TradeExitTimePolicy cannot be null."
            );
        }

        this.openTrades = new ArrayList<>();
        this.tradeExitTimePolicy = tradeExitTimePolicy;
    }

    /**
     * Returns all currently open trades.
     *
     * @return immutable copy of open trades
     */
    @Override
    public synchronized List<RoundTripTrade> getOpenTrades() {
        return List.copyOf(openTrades);
    }

    /**
     * Returns open trades whose planned exit time is at or before the supplied
     * timestamp.
     *
     * @param timestamp timestamp to compare against planned exit times
     * @return immutable list of trades due for exit
     * @throws IllegalArgumentException if {@code timestamp} is null
     */
    @Override
    public synchronized List<RoundTripTrade> getTradesDueForExit(
            Instant timestamp
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be null."
            );
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
            throw new IllegalArgumentException(
                    "Trade cannot be null."
            );
        }

        if (!openTrades.remove(trade)) {
            throw new IllegalStateException(
                    "Trade is not currently open: " + trade
            );
        }
    }

    /**
     * Closes each supplied trade by removing it from the registry.
     *
     * <p>Trades are closed in iteration order. If validation or removal fails,
     * subsequent trades are not processed.
     *
     * @param trades trades to close
     * @throws IllegalArgumentException if {@code trades} is null or contains null
     * @throws IllegalStateException if any trade is not currently open
     */
    public synchronized void closeAll(List<RoundTripTrade> trades) {
        if (trades == null) {
            throw new IllegalArgumentException(
                    "Trades cannot be null."
            );
        }

        for (RoundTripTrade trade : trades) {
            close(trade);
        }
    }

    /**
     * Adds a validated open trade to the registry.
     *
     * @param trade trade to add
     * @throws IllegalArgumentException if {@code trade} is null or has an
     *         invalid planned exit time
     * @throws IllegalStateException if the trade is already open
     */
    public synchronized void add(RoundTripTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException(
                    "Trade cannot be null."
            );
        }

        validateTradeTiming(trade);

        if (openTrades.contains(trade)) {
            throw new IllegalStateException(
                    "Trade is already open: " + trade
            );
        }

        openTrades.add(trade);
    }

    /**
     * Adds each supplied trade to the registry.
     *
     * <p>Trades are added in iteration order. If validation or insertion fails,
     * subsequent trades are not processed.
     *
     * @param trades trades to add
     * @throws IllegalArgumentException if {@code trades} is null, contains null,
     *         or contains a trade with invalid timing
     * @throws IllegalStateException if any trade is already open
     */
    public synchronized void addAll(List<RoundTripTrade> trades) {
        if (trades == null) {
            throw new IllegalArgumentException(
                    "Trades cannot be null."
            );
        }

        for (RoundTripTrade trade : trades) {
            add(trade);
        }
    }

    /**
     * Applies an exit-time adjustment to an open trade.
     *
     * <p>The target trade is replaced with an equivalent trade having the
     * adjusted exit time. The resulting trade must satisfy the configured
     * {@link TradeExitTimePolicy}.
     *
     * @param adjustment adjustment to apply
     * @throws IllegalArgumentException if {@code adjustment} is null or its
     *         resulting exit time is invalid
     * @throws IllegalStateException if the target trade is not currently open
     */
    public synchronized void adjust(OpenTradeAdjustment adjustment) {
        if (adjustment == null) {
            throw new IllegalArgumentException(
                    "Adjustment cannot be null."
            );
        }

        RoundTripTrade targetTrade = adjustment.targetTrade();

        if (targetTrade == null) {
            throw new IllegalArgumentException(
                    "Adjustment target trade cannot be null."
            );
        }

        if (adjustment.newExitTime() == null) {
            throw new IllegalArgumentException(
                    "Adjusted exit time cannot be null."
            );
        }

        int index = openTrades.indexOf(targetTrade);

        if (index < 0) {
            throw new IllegalStateException(
                    "Trade is not currently open: " + targetTrade
            );
        }

        if (!adjustment.newExitTime().isAfter(targetTrade.exitTime())) {
            throw new IllegalArgumentException(
                    "Adjusted exit time must be later than the current exit time. "
                            + "Current exit: "
                            + targetTrade.exitTime()
                            + ", adjusted exit: "
                            + adjustment.newExitTime()
            );
        }

        RoundTripTrade adjustedTrade = new RoundTripTrade(
                targetTrade.ticker(),
                targetTrade.quantity(),
                targetTrade.entryTime(),
                adjustment.newExitTime(),
                targetTrade.strategyId()
        );

        validateTradeTiming(adjustedTrade);

        openTrades.set(index, adjustedTrade);
    }

    /**
     * Applies each supplied adjustment.
     *
     * <p>Adjustments are applied in iteration order. If validation or
     * replacement fails, subsequent adjustments are not processed.
     *
     * @param adjustments adjustments to apply
     * @throws IllegalArgumentException if {@code adjustments} is null, contains
     *         null, or contains an invalid adjustment
     * @throws IllegalStateException if any target trade is not currently open
     */
    public synchronized void adjustAll(
            List<OpenTradeAdjustment> adjustments
    ) {
        if (adjustments == null) {
            throw new IllegalArgumentException(
                    "Adjustments cannot be null."
            );
        }

        for (OpenTradeAdjustment adjustment : adjustments) {
            adjust(adjustment);
        }
    }

    /**
     * @return true if any round-trip trades are open, false if none are open
     */
    @Override
    public boolean hasOpenTrades() {
        return !openTrades.isEmpty();
    }

    /**
     * Validates the timing constraints of a trade before it is stored.
     *
     * @param trade trade to validate
     * @throws IllegalArgumentException if the trade's timing is invalid
     */
    private void validateTradeTiming(RoundTripTrade trade) {
        Instant entryTime = trade.entryTime();
        Instant exitTime = trade.exitTime();

        if (entryTime == null) {
            throw new IllegalArgumentException(
                    "Trade entry time cannot be null."
            );
        }

        if (exitTime == null) {
            throw new IllegalArgumentException(
                    "Trade exit time cannot be null."
            );
        }

        if (!exitTime.isAfter(entryTime)) {
            throw new IllegalArgumentException(
                    "Trade exit time must be after entry time. Entry: "
                            + entryTime
                            + ", exit: "
                            + exitTime
            );
        }

        Instant latestAllowedExit =
                tradeExitTimePolicy.latestAllowedExit(entryTime);

        if (exitTime.isAfter(latestAllowedExit)) {
            throw new IllegalArgumentException(
                    "Trade exit time exceeds the latest allowed exit. Exit: "
                            + exitTime
                            + ", latest allowed exit: "
                            + latestAllowedExit
            );
        }
    }
}