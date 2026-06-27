package com.algotrader.runtime;

import com.algotrader.decision.dataobjects.RoundTripTrade;

/**
 * Describes a mutation that should be applied to an
 * {@link OpenTradeRegistry}.
 *
 * <p>An update may add a newly opened trade, remove a completed or cancelled
 * trade, or replace an existing trade with an updated version (for example,
 * to extend its planned exit time).
 *
 * @param type type of registry update
 * @param trade trade to add, remove, or replace
 * @param replacementTrade replacement trade for {@link OpenTradeRegistryUpdateType#REPLACE};
 *                         {@code null} for all other update types
 */
public record OpenTradeRegistryUpdate(
        OpenTradeRegistryUpdateType type,
        RoundTripTrade trade,
        RoundTripTrade replacementTrade) {

    /**
     * Creates an immutable registry update.
     *
     * @throws IllegalArgumentException if the supplied fields are inconsistent
     *         with the requested update type
     */
    public OpenTradeRegistryUpdate {
        if (type == null) {
            throw new IllegalArgumentException("Update type cannot be null.");
        }

        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null.");
        }

        switch (type) {
            case ADD, REMOVE -> {
                if (replacementTrade != null) {
                    throw new IllegalArgumentException(
                            "Replacement trade must be null for "
                                    + type
                                    + " updates.");
                }
            }

            case REPLACE -> {
                if (replacementTrade == null) {
                    throw new IllegalArgumentException(
                            "Replacement trade is required for REPLACE updates.");
                }
            }
        }
    }
}