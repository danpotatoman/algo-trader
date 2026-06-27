package com.algotrader.runtime;

/**
 * Types of updates that may be applied to an {@link OpenTradeRegistry}.
 */
public enum OpenTradeRegistryUpdateType {

    /**
     * Adds a new open trade to the registry.
     */
    ADD,

    /**
     * Removes an existing open trade from the registry.
     */
    REMOVE,

    /**
     * Replaces an existing open trade with an updated version.
     */
    REPLACE
}