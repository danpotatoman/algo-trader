# Findings and presentation classification

This is a fixed 59-session exploratory evaluation, not strategy selection. Gross strategy return was **1.804%**, versus **2.299%** for identically bounded SPY price-only buy-and-hold: **-0.495 percentage points**. Engineering and auditability are the strongest resume evidence.

## A. Resume-candidate results

These are claim ingredients, not final resume bullets.

### CPU batch prediction latency

- Exact metric: `{"median_ms": 9.9791, "p95_ms": 11.74042}`
- Calculation: numpy.quantile(predictionDurationNanos / 1e6, [0.5, 0.95]) on COMPLETED inference cycles only
- Denominator: 2,773 inference requests; 15 tickers each; 41,595 ticker predictions
- Qualification: One local CPU backtest, serial calls, fixed batch size. Includes first inference; excludes 1,798 empty-window cycles. HTTP round trip includes mapping, models and parsing. Not standalone single-ticker or pure neural-kernel latency.
- Why defensible: Every included observation and workload is logged; percentiles can be reproduced without rerunning models.
- Artifact fields: session.json: cycleLogs[].evaluation.inferenceStatus; cycleLogs[].evaluation.inferenceBatchSize; cycleLogs[].evaluation.predictionDurationNanos; manifest.json: runtime and predictionService.runtime

### End-to-end historical throughput

- Exact metric: `{"seconds": 30.7423115, "predictions_per_second": 1353.0212261364927}`
- Calculation: completion.fullBacktestWallNanos / 1e9; 41,595 / resulting seconds
- Denominator: 4,571 cycles, 2,773 inference batches, 41,595 predictions, 545 completed trades, 59 market sessions
- Qualification: Already-running Python service; excludes process startup, training and data download. Includes run setup/provenance, snapshot, execution and session serialization. One observed run, not a sustained load benchmark.
- Why defensible: Explicit monotonic timing scope and exact workload; warm-up cycles are not counted as ticker predictions.
- Artifact fields: completion.json: fullBacktestWallNanos, cycleCount; session.json: cycleLogs and execution records

### Auditable multi-ticker execution

- Exact metric: `{"reconciled_round_trips": 545, "stable_id_extensions": 4550, "current_marks": 8166, "failed_executions": 0, "all_cash_position_registry_checks_passed": true}`
- Calculation: Join entries, exits and adjustments by tradeId; verify every entry closes once; reconcile quantities, fills, marked equity and terminal flat state
- Denominator: 545 entries, 545 exits, 4,550 applied extensions, 4,571 cycle valuations plus initial/final valuation
- Qualification: Historical paper execution with exact candle-open fills and no modeled costs. Zero failures describes this run, not production availability or future failure probability.
- Why defensible: Stable IDs, cash/position reconciliation, exact snapshot prices, immutable inputs and matching checksums support independent audit.
- Artifact fields: session.json: successfullyOpenedTrades, successfulEntryExecutions, successfullyClosedTrades, successfulExitExecutions, appliedOpenTradeAdjustments; portfolioAfterCycle, finalPortfolio, liquidationResult, remainingOpenTrades; manifest.json: data.sha256, git.sourceFileSha256; completion.json: sessionSha256


## B. Portfolio / README results

- **Modest gross profitability, with benchmark underperformance:** $180.44 on $10000; 286/545 winning trades (52.48%), profit factor 1.124. This is descriptive, before costs. The session-block 95% interval for mean daily return spans -0.0446% to 0.1046%; it includes zero.
- **Lower observed drawdown with substantially less exposure:** strategy -2.222% versus SPY -3.661%; mean cycle exposure 21.53%. SPY holds overnight; strategy does not. This is not a matched-risk comparison or proof of superior risk management.
- **Classifier discrimination:** ROC AUC 0.6547; 95% five-session-block interval [0.6377, 0.6701] over 37,170 labeled predictions across 59 sessions. Brier 0.130962 versus 0.136491 for an ex-post sample-prevalence constant (4.05% relative improvement). Predictions are overlapping, not independent trials.
- **Important classifier caveat:** probability and predicted volatility have rank correlation 0.808. Predicted volatility alone ranks the classifier event with AUC 0.6746; classifier minus volatility-score AUC is -0.0199 (block interval [-0.0298, -0.0089]). This is a diagnostic of saved scores, not a new strategy. A positive-return threshold becomes easier to exceed in either-direction volatile markets; ranking this event is not the same as predicting a profitable direction.
- **Volatility model contains useful descriptive signal after unit conversion:** predicted/realized correlation 0.646, MAE 0.00029907 return units, 33.01% below the endpoint-metadata constant-mean baseline. Logged forecasts are in training units (target multiplied by 1,000); divide by 1,000 only for evaluation. The actual Java allocator used the logged scale unchanged. This prevents promoting correct volatility-calibrated live allocation from this run.
- **Trade behavior:** 457/545 trades were extended; median holding 55 minutes versus the initial 30-minute plan. Non-extended trades: 88, 69.32% wins, $104.05 P&L; extended: 457, 49.23% wins, $76.39 P&L. Extension is conditional on subsequent signals: this comparison cannot establish that extensions help or hurt causally.
- **Concentration:** AMD contributes $160.51, leaving $19.93 from other tickers. The five largest winners account for 13.75% of gross winning P&L but 124.58% of net P&L. Removing their accounting contributions leaves $-44.36; this is attribution, not a rerun without those trades.
- **Subgroups are not a tuning recommendation:** AAPL and AMD contributed positively; TSLA was the largest negative contributor. COST (2 trades) and SPY (3) have 100% observed trade win rates, which are too small to promote. The highest probability bucket has only 61 labeled predictions and a negative mean fixed-target return. Tables retain every ticker, all observed probability/volatility buckets, their interaction, decisions, extensions, and zero-volume overlap.
- **Runtime is dominated by prediction:** prediction round trips total 28.046s; evaluation is 99.61% of summed cycle work. First inference is 81.960ms and remains included in the headline latency distribution. Entry-work groups have similar median cycle latency (about 10ms); tails differ and include startup. All inference batches are size 15, so batch-size scaling cannot be inferred.
- **Data limitation retained:** all 21 zero-volume candles are preserved. They occur in 420 prediction windows (361 labeled), across 372 cycles. Subgroup diagnostics are disclosed without deleting records or changing the strategy. Differences cannot establish a causal effect of bad volume.

## C. Results not suitable for promotion

- Outperformance or statistically established trading alpha: SPY return was higher; daily excess-return and mean-trade-P&L block intervals include zero. The descriptive annualized daily Sharpe (1.47) comes from only 59 sessions and is unsuitable as a robust headline.
- Expected live profitability: linear break-even cost is only 1.408 bps per side. At 5 bps on each recorded fill, fixed-fill arithmetic gives $-460.38. This does not model cost-induced changes in capital or decisions and is not a rerun.
- Claims of calibrated volatility-aware allocation: the serving/strategy path uses forecasts scaled by 1,000. No code or fills were corrected for this analysis. The model diagnostics use converted units; simulated trade results remain as executed.
- High accuracy without class balance, high-confidence tiny subgroups, individual winning tickers, or extended/non-extended comparisons as validated strategies. The all-negative classifier baseline is already about 83.69% accurate. Many observations share labels, tickers and market conditions.
- Sub-millisecond single-ticker latency: dividing a batch's latency by 15 is amortized work, not the response time of a single-ticker request. No production SLO, scaling law, concurrency claim, download-inclusive runtime or speedup over an unmeasured baseline is established.

## Five strongest overall facts

1. A reproducible Java/Python CPU run processed 15 tickers, 41,595 ticker predictions and 545 completed trades in 30.742s of recorded application time.
2. Full 15-ticker prediction round trips had 9.979ms median and 11.740ms p95, using all 2,773 actual inference calls.
3. The artifact supports a full audit: 545 round trips, 4,550 extensions, exact-price marked equity, no failed executions or stale marks, and a flat terminal portfolio.
4. Saved predictions contain measurable out-of-sample descriptive structure (classifier AUC 0.655; converted volatility correlation 0.646), but volatility dependence and the serving-scale issue limit stronger claims.
5. The honest trading result is a small gross gain (1.80%) with lower exposure/drawdown, but benchmark underperformance, concentration and very limited cost headroom—not evidence of a deployable trading edge.

All claims are conditional on the documented execution assumptions, fixed ticker universe, 59 sessions, retained volume anomalies, and exact immutable run. No final resume bullet is written here.
