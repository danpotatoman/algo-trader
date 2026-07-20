package com.algotrader.decision.generator.validation;

import java.time.Instant;

import com.algotrader.decision.dataobjects.RoundTripTrade;
import com.algotrader.decision.generator.TradeExitTimePolicy;

/**
 * Validates whether a {@link RoundTripTrade} satisfies configured trade timing
 * constraints.
 *
 * <p>This validator delegates market-close and trading-session boundaries to
 * {@link TradeExitTimePolicy}.
 *
 * <p>Validation rules include:
 * <ul>
 *     <li>The exit time must occur after the entry time</li>
 *     <li>The exit time must not be later than the latest exit permitted by
 *         the trade exit-time policy</li>
 * </ul>
 */
public final class RoundTripTradeValidator {

    private final TradeExitTimePolicy tradeExitTimePolicy;

    /**
     * Creates a round-trip trade validator.
     *
     * @param tradeExitTimePolicy policy used to determine the latest valid exit
     *        time for a trade
     * @throws IllegalArgumentException if {@code tradeExitTimePolicy} is null
     */
    public RoundTripTradeValidator(
            TradeExitTimePolicy tradeExitTimePolicy
    ) {
        if (tradeExitTimePolicy == null) {
            throw new IllegalArgumentException(
                    "TradeExitTimePolicy cannot be null."
            );
        }

        this.tradeExitTimePolicy = tradeExitTimePolicy;
    }

    /**
     * Determines whether a round-trip trade satisfies all configured timing
     * constraints.
     *
     * @param trade trade to validate
     * @return {@code true} if the trade is valid; {@code false} otherwise
     * @throws IllegalArgumentException if {@code trade} is null
     */
    public boolean isValid(
            RoundTripTrade trade
    ) {
        if (trade == null) {
            throw new IllegalArgumentException(
                    "RoundTripTrade cannot be null."
            );
        }

        Instant entryTime = trade.entryTime();
        Instant exitTime = trade.exitTime();

        if (!exitTime.isAfter(entryTime)) {
            return false;
        }

        Instant latestAllowedExit =
                tradeExitTimePolicy.latestAllowedExit(entryTime);

        return !exitTime.isAfter(latestAllowedExit);
    }
}