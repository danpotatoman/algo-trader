package com.algotrader.decision.dataobjects;

import java.time.Instant;

/**
 * Represents intent from the decision layer to execute a trade.
 *
 * <p>Implementations describe the trade side, target ticker, sizing input,
 * and execution timestamp without performing execution themselves.
 */
public sealed interface TradeInstruction
        permits BuyInstruction, SellInstruction {

    String ticker();

    Instant executionTime();
}
