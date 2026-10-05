# Corrected showcase findings

Canonical run `2ef02c28-189c-447a-af1e-880d15f9e575`. The legacy run is used only for the labeled engineering comparison. No strategy selection or parameter tuning.


## A. Strong resume candidates

Claim ingredients, not final resume bullets.


### CPU batch prediction latency


- value: {"median_ms": 8.2659, "p95_ms": 11.13028}

- calculation: numpy.quantile(predictionDurationNanos / 1e6, [0.5, 0.95]) on COMPLETED inference cycles only

- denominator: 2,773 inference requests; 15 tickers each; 41,595 ticker predictions

- artifact_fields: ["session.json: cycleLogs[].evaluation.inferenceStatus", "cycleLogs[].evaluation.inferenceBatchSize", "cycleLogs[].evaluation.predictionDurationNanos", "manifest.json: runtime and predictionService.runtime"]

- qualification: One local CPU backtest, serial calls, fixed batch size. Includes first inference; excludes 1,798 empty-window cycles. HTTP round trip includes mapping, models and parsing. Not standalone single-ticker or pure neural-kernel latency.

- why_defensible: Every included observation and workload is logged; percentiles can be reproduced without rerunning models.

### End-to-end historical throughput


- value: {"seconds": 27.2949995, "predictions_per_second": 1523.9055051090952}

- calculation: completion.fullBacktestWallNanos / 1e9; 41,595 / resulting seconds

- denominator: 4,571 cycles, 2,773 inference batches, 41,595 predictions, 545 completed trades, 59 market sessions

- artifact_fields: ["completion.json: fullBacktestWallNanos, cycleCount", "session.json: cycleLogs and execution records"]

- qualification: Already-running Python service; excludes process startup, training and data download. Includes run setup/provenance, snapshot, execution and session serialization. One observed run, not a sustained load benchmark.

- why_defensible: Explicit monotonic timing scope and exact workload; warm-up cycles are not counted as ticker predictions.

### Auditable multi-ticker execution


- value: {"reconciled_round_trips": 545, "stable_id_extensions": 4550, "current_marks": 8166, "failed_executions": 0, "all_cash_position_registry_checks_passed": true}

- calculation: Join entries, exits and adjustments by tradeId; verify every entry closes once; reconcile quantities, fills, marked equity and terminal flat state

- denominator: 545 entries, 545 exits, 4,550 applied extensions, 4,571 cycle valuations plus initial/final valuation

- artifact_fields: ["session.json: successfullyOpenedTrades, successfulEntryExecutions, successfullyClosedTrades, successfulExitExecutions, appliedOpenTradeAdjustments", "portfolioAfterCycle, finalPortfolio, liquidationResult, remainingOpenTrades", "manifest.json: data.sha256, git.sourceFileSha256; completion.json: sessionSha256"]

- qualification: Historical paper execution with exact candle-open fills and no modeled costs. Zero failures describes this run, not production availability or future failure probability.

- why_defensible: Stable IDs, cash/position reconciliation, exact snapshot prices, immutable inputs and matching checksums support independent audit.


## B. README / portfolio discussion


- Gross return 1.6324% ($163.2407) versus SPY 2.2991%. Win rate 286/545 (52.477%); profit factor 1.1108. Maximum drawdown -2.3364% versus SPY -3.6613%. Average exposure 21.652%, peak 88.730%. SPY holds overnight; this is not a matched-risk comparison.

- Classifier threshold-event ROC AUC 0.654661, five-session-block 95% interval [0.637728, 0.670067], n=37,170 across 59 sessions. Volatility alone event AUC 0.674607; probability/volatility Spearman 0.808446.

- Directional-return AUC 0.501466; absolute-move AUC 0.677568. Probability also ranks negative-threshold events at 0.629764. This supports movement-magnitude-associated discrimination, not demonstrated directional skill or a causal claim about the network's internal mechanism.

- Canonical volatility: Pearson correlation 0.646491, Spearman 0.670935; MAE 0.000299068, versus 0.000446433 for the endpoint constant (33.009% lower). The baseline's estimation dataset is not certified as training-only. No second division by 1,000.

- Holding behavior: 457/545 trades extended, median 55 minutes. Extension-group results below are observational because extensions depend on later signals.

- Numerical convention: strict cash-P&L signs give 286 wins, 258 losses and one exactly flat trade. A second trade has identical entry/exit prices but -2.842e-14 dollars cash-rounding residue; economically there are two flat-price trades. This is not a new losing market outcome. Win rate and material P&L are unaffected.

| group | trades | win_rate | total_pnl | profit_factor | mean_return |
|---|---|---|---|---|---|
| False | 88 | 0.693182 | 103.512 | 2.29917 | 0.00106426 |
| True | 457 | 0.492341 | 59.7283 | 1.04287 | -1.19902e-05 |

- Concentration: largest contributor AMD ($152.91); other tickers together $10.33. Top five winners contribute 14.28% of gross wins and 143.16% of net P&L. Subtracting their contributions leaves $-70.45; this is accounting, not a strategy without those trades.

- Runtime: prediction round trips consume 24.496s; evaluation accounts for 99.59% of cycle work. All batches contain 15 tickers, so there is no measured batch-size scaling relationship. Workload groups and latency correlations are retained in the full report/metrics.

- Auditable contract repair: 543/545 entry requests changed across actual trajectories, while signal eligibility and event timing remained invariant. See [the engineering comparison and case study](comparison.md). Financial differences do not validate the strategy.


## C. Unsuitable for promotion


- Established alpha, expected live profitability, or superior risk management. Costs/spread/slippage are zero; only 59 sessions and a fixed universe are observed. Mean daily return 95% block interval [-0.04739%, 0.10205%]; excess-return interval [-0.17292%, 0.13190%].

- Robust cost tolerance: fixed-fill break-even is 1.2827 bps per side. Annualized Sharpe 1.317 is a short-sample descriptive statistic, not a dependable headline.

| one_way_bps | hypothetical_pnl |
|---|---|
| 1 | 35.9769 |
| 5 | -473.078 |
| 10 | -1109.4 |

- Directional skill, independently validated profitable probability/volatility subgroups, tiny-ticker win rates, or causal benefits of extensions. Overlapping labels, shared shocks and exploratory subgroup search preclude those interpretations. No multiple-comparison-adjusted subgroup claims are made.

- Canonical units alone do not establish calibrated portfolio-risk sizing or protective causality. This repair restored the serving contract; the allocation formula was neither redesigned nor optimized.

- Production reliability/SLOs, sustained throughput, conversion-induced speedup or slowdown, or single-request latency obtained by dividing batch latency by 15. These are two local serial runs with already-running model services.


## Five strongest overall facts


1. The corrected Java/Python CPU application processed 41,595 predictions in 27.294999s (1523.906/s), covering 4,571 cycles and 545 completed trades.

2. Across 2,773 actual 15-ticker requests, median prediction round trip was 8.2659ms and p95 11.1303ms; intentional morning skips are excluded.

3. Stable trade IDs and immutable artifacts reconcile 545 round trips, 4,550 extensions and 8,166 current position marks, with no failed executions and a flat terminal portfolio.

4. Saved artifacts made an end-to-end unit-contract defect traceable and its regression-tested correction verifiable on the same predetermined period, preserving signal-path invariants.

5. Model diagnostics show descriptive structure: canonical-volatility realized correlation 0.646 and event AUC 0.655; directional AUC remains 0.501. This is useful model evaluation, not a demonstrated live trading edge.


All results retain 21 zero-volume vendor candles (420 input windows, 372 cycles), zero modeled transaction costs, overlapping horizons, a fixed ticker universe and a 59-session sample. Subgroups are descriptive, not independent strategies. No final resume bullets are provided.
