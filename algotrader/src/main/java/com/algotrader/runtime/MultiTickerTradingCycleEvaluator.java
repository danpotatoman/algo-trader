package com.algotrader.runtime;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.algotrader.decision.dataobjects.PortfolioDecisionResult;
import com.algotrader.decision.generator.PortfolioDecisionGenerator;
import com.algotrader.logging.MultiTickerCycleEvaluation;
import com.algotrader.marketdata.cache.DataCacheException;
import com.algotrader.marketdata.model.DataBatch;
import com.algotrader.marketdata.provider.MultiTickerWindowProvider;
import com.algotrader.portfolio.PortfolioSnapshot;
import com.algotrader.portfolio.PortfolioView;

/**
 * Evaluates a single multi-ticker trading cycle.
 *
 * <p>This class collects all valid market-data windows for the supplied cycle
 * time, captures the current portfolio snapshot, and delegates trading logic
 * to a {@link PortfolioDecisionGenerator}.
 *
 * <p>The evaluator does not execute actions, mutate portfolio state, or update
 * the open-trade registry. It only returns the intended portfolio actions and
 * registry updates that the caller may apply.
 */
public final class MultiTickerTradingCycleEvaluator {

    private final MultiTickerWindowProvider windowProvider;
    private final PortfolioDecisionGenerator portfolioDecisionGenerator;
    private final PortfolioView portfolioView;

    /**
     * Creates a multi-ticker cycle evaluator.
     *
     * @param windowProvider provider used to construct market data windows
     * @param portfolioDecisionGenerator generator used to evaluate decisions
     * @param portfolioView read-only portfolio view used during evaluation
     * @throws IllegalArgumentException if any dependency is null
     */
    public MultiTickerTradingCycleEvaluator(
            MultiTickerWindowProvider windowProvider,
            PortfolioDecisionGenerator portfolioDecisionGenerator,
            PortfolioView portfolioView) {

        if (windowProvider == null) {
            throw new IllegalArgumentException(
                    "Window provider cannot be null.");
        }

        if (portfolioDecisionGenerator == null) {
            throw new IllegalArgumentException(
                    "Portfolio decision generator cannot be null.");
        }

        if (portfolioView == null) {
            throw new IllegalArgumentException(
                    "Portfolio view cannot be null.");
        }

        this.windowProvider = windowProvider;
        this.portfolioDecisionGenerator = portfolioDecisionGenerator;
        this.portfolioView = portfolioView;
    }

    /**
     * Initializes the underlying market data window provider.
     *
     * @throws DataCacheException if initialization fails
     */
    public void initialize() throws DataCacheException {
        windowProvider.initialize();
    }

    /**
     * Evaluates one multi-ticker trading cycle at the supplied timestamp.
     *
     * @param cycleTime trading cycle timestamp
     * @return evaluation result containing intended actions and registry
     *         updates
     * @throws IllegalArgumentException if {@code cycleTime} is null
     * @throws RuntimeException if window retrieval or decision generation fails
     */
    public MultiTickerCycleEvaluation evaluate(Instant cycleTime) {
        if (cycleTime == null) {
            throw new IllegalArgumentException("Cycle time cannot be null.");
        }

        long startNanos = System.nanoTime();

        try {
            List<DataBatch> batches = windowProvider.windowsAt(cycleTime);

            PortfolioSnapshot portfolioSnapshot =
                    portfolioView.snapshot();

            PortfolioDecisionResult decisionResult =
                    portfolioDecisionGenerator.generateDecision(
                            batches,
                            portfolioSnapshot);

            long cycleDurationMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startNanos);

            return new MultiTickerCycleEvaluation(
                    cycleTime,
                    cycleDurationMillis,
                    portfolioSnapshot,
                    decisionResult.actions(),
                    decisionResult.registryUpdates());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Multi-ticker trading cycle evaluation failed at "
                            + cycleTime,
                    e);
        }
    }
}
