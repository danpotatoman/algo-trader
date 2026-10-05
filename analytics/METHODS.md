# Fixed showcase analytics: regeneration and definitions

For the canonical corrected run and the preserved legacy-versus-corrected
engineering comparison, use [METHODS_CORRECTED.md](METHODS_CORRECTED.md) and
`python analytics/analyze_corrected.py`. The definitions below document the
legacy analysis and supply shared calculation methodology.

This pipeline analyzes only run `4b966e0f-d9c1-4de3-8a09-5d4b5af44332`.
That immutable run contains legacy **training-unit** volatility outputs. Its
allocation results describe the original unit-mismatched implementation, not
the repaired allocator input contract. Existing reports are preserved.

Volatility decoding now uses `predictionService.volatilityOutputContract`:
`volatility-return-fraction-v1` with `units=return_fraction` and
`inverseTargetScalingApplied=true` means no further division. Only the known
legacy run and volatility-model hash are allowed to omit that contract; their
outputs are divided by the checkpoint's validated `target_scale`. Unknown or
missing contracts fail rather than guessing. This decoder supports corrected
units, but the full report pipeline remains restricted to this fixed legacy run;
enabling another run's reporting is a separate task.

It does not call the prediction server, executor, ingestion service or strategy,
and does not modify source run files, model checkpoints or configuration.

From the repository root, with the project's Python environment:

```powershell
.\.venv\Scripts\python.exe -m pip install -r analytics/requirements.txt
.\.venv\Scripts\python.exe -m unittest discover -s analytics/tests -v
.\.venv\Scripts\python.exe analytics/analyze_showcase.py
```

The libraries were already installed during this analysis; no package upgrade was
needed. Default output is `reports/showcase/4b966e0f-d9c1-4de3-8a09-5d4b5af44332/`.
An alternative **derived-output** directory can be selected with `--output PATH`.
The source path can be relocated with `--run PATH`; the run ID remains fixed.
The script rejects output inside the run directory or an ancestor of it.

Outputs:

- `metrics.json`: complete machine-readable statistics, distributions, groups,
  bootstrap intervals, environment, script hash and all four input-file hashes.
- `report.md`: broad results, all diagnostic tables, uncertainty and charts.
- `findings.md`: narrative, unfavorable findings, A/B/C classifications and five strongest facts.
- `resume_candidates.json`: exact claim ingredients, calculations, denominators,
  source fields, qualifications and justification; no final resume bullet.
- `cycles.jsonl`, `predictions.jsonl`, `trades.jsonl`, `sessions.jsonl`:
  observation-level derived records for independent reproduction.
- Four PNG charts: equity/drawdown, attribution/exposure, model diagnostics,
  engineering latency/stages.
- `analysis_completion.json`: input-unchanged assertion and hashes of derived outputs.

SQLite is opened with `mode=ro&immutable=1`. The session and snapshot hashes are
checked against completion/manifest records. All original run files are hashed
again after analysis. Keep those inputs and the matching checkout: the original
run reports a dirty working tree, so its Git commit alone does not reproduce it.
Target-definition source hashes are checked against the manifest before outputs
are published. Financial arithmetic uses binary floating point and reconciliation
tolerances; JSON non-finite values become `null`. Statistical percentiles use
NumPy's default linear interpolation. Bootstrap seed is fixed at 20260930.

## Equity, SPY, drawdown and session returns

Both frozen checkpoints report training cutoff 2026-05-01T19:40:00Z and
validation cutoff 2026-05-18T19:45:00Z. The earliest input candle here opens at
2026-07-07T13:30:00Z, after both cutoffs. Model identities/hashes and these cutoffs
are copied into the derived metrics provenance.

Authoritative strategy observations are
`session.cycleLogs[].portfolioAfterCycle` at `POST_CYCLE`, after exits, evaluation,
entries and extensions. Each must be `COMPLETE` with exact `CURRENT` marks.
Recompute equity as cash plus sum(quantity times price); include initial equity
as the starting anchor. Terminal marked equity must equal the last post-cycle
equity for this already-flat run. Total return is terminal/initial minus one.
The sum of completed-trade P&L must equal terminal minus initial equity.

The benchmark invests the same $10,000 in fractional SPY shares at frozen
`FIVE_MINUTES.open` for **2026-07-07T16:05:00Z**, holds the shares, and liquidates
at the open for **2026-09-28T19:55:00Z**. Starting/ending prices and share quantity
are in `metrics.financial.benchmark`. Benchmark equity at every strategy cycle
is initial capital times SPY.open(t)/SPY.open(start). Apply the same zero
commission, spread, slippage and execution-delay assumptions; no dividends,
cash interest or additional corporate-action accounting. These are the vendor
OHLC prices supplied with `auto_adjust=False`, not a total-return series.
SPY remains exposed overnight; the strategy is intraday and mostly cash.
No leverage/exposure matching or benchmark parameter fitting is performed.

Drawdown(t) = equity(t)/max(initial equity, all equity through t) minus one.
Maximum drawdown is its minimum. Peak/trough/recovery are observed cycle times;
drawdown duration counts underwater market samples, **not continuous elapsed
minutes** across nights. This sampling cannot observe every intrabar or
overnight high/low, particularly for SPY. Each session return is final sampled
equity divided by previous session final sampled equity minus one; the first
session uses initial capital and is truncated at the start. Annualized daily
Sharpe is mean(return)/sample-standard-deviation(return) times sqrt(252),
zero risk-free assumption. Annualized volatility uses the same sample standard
deviation. These are short-sample descriptive statistics, not forecasts.
Beta = sample covariance(strategy,SPY)/sample variance(SPY); no causal alpha claim.

## Trades, exposure, concentration and costs

Join `successfulEntryExecutions` and `successfulExitExecutions` by `tradeId`,
including any terminal liquidation fills. Quantity and identities must agree.
P&L = exit cashAmount minus entry cashAmount; trade return = exit price/entry
price minus one. Holding time uses actual execution timestamps, including every
applied extension with the same ID. Do not compare extended returns directly to
30-minute classification labels. Win rate counts strictly positive P&L over all
545 trades, including two flat trades in the denominator. Profit factor is
gross winning P&L divided by absolute gross losing P&L. Mean trade return is
equal-trade weighted; entry-capital-weighted return is total P&L/sum(entry notional).

MFE/MAE are extrema of candle highs/lows over [entry, exit), plus initial and
exit prices, relative to entry. Exclude the exit candle's later high/low to avoid
using movement after the trade closed. These are bar-resolution excursions.
Win/loss streaks follow entry-log order; simultaneous entries follow their logged
order. They are not independent sequential betting trials.

Exposure = marked holdings/equity. Report equally weighted post-cycle snapshots,
not continuous wall-clock exposure. Single-ticker and top-three weights divide
marked values by equity. Invested HHI = sum((position value/total invested)^2),
defined only while invested. Turnover = sum(buy and sell notionals)/mean sampled
equity. Correlations between exposure and drawdown/returns are descriptive,
contemporaneous, and not tests of protective causality.

Top-trade and ticker concentration are accounting contributions. Subtracting a
contribution does not simulate removing that trade/ticker or recycling capital.
Cost sensitivity subtracts `bps/10000 * sum(buy and sell notionals)` from gross
P&L at 1, 5 and 10 bps **per side**. Break-even bps = gross P&L/total notional *
10000. No fills, decisions or allocation sizes are rerun; results do not model
costs changing subsequent cash availability and are not expected live results.

## Classifier, volatility and realized outcomes

The recorded endpoint supplies 31 consecutive completed candles: one context
candle plus 30 model candles. At cycle t, the last completed candle **opens at
t-5 minutes and closes at t**. The training target is:

```
r30 = close(candle-open t+25m) / close(candle-open t-5m) - 1
y = 1[r30 >= 0.002]
vol30 = population_std(six consecutive close-to-close five-minute returns)
```

This is the exact target in the hashed training builders, tested directly against
them on synthetic data. It is not “price rose 0.2% at any point,” daily volatility,
annualized volatility, or an extended trade's realized return.
Require all seven endpoint/intermediate closes at exact five-minute spacing.
There are **37,170 labeled predictions**; **4,425** lack a complete future horizon
(five final inference cycles per session * 59 * 15). Do not substitute next-day
bars or impute outcomes. Fifteen labels from the last evaluation day mature at
20:00 UTC, five minutes after the final execution, using the final frozen candle's
close. They are outcome labels only, not extra strategy cycles or benchmark prices.

An additional executable-price diagnostic is open(t+30m)/open(t)-1, where both
exact opens exist: 36,285 predictions. It has a different reference price and
availability from the intended model target. Actual trade outcomes are joined
separately by entry signal and stable trade ID and may last up to 230 minutes.

Probability metrics: Brier = mean((p-y)^2); log loss uses probabilities clipped
to [1e-12,1-1e-12]; ROC AUC is the positive/negative pair concordance with half
credit for ties. Constant-prevalence Brier = prevalence*(1-prevalence), where
prevalence is estimated on this evaluation sample. This is an ex-post descriptive
baseline, **not a deployable training-only predictor**. Precision/recall/confusion
matrices are shown at the existing 0.25 strategy threshold and conventional 0.5
classification cutoff, without rerunning any trades. Accuracy must be compared
with the majority-class baseline because positives are uncommon.

**Volatility units issue:** training multiplies targets by 1,000; both frozen
model provenance and hashed training source record this. For this legacy run,
the server returned raw model output unchanged, and the Java score used
probability edge/(1+logged volatility). The analysis keeps logged forecasts and
divides by the recorded target scale (1,000 for this checkpoint) solely
for comparison to realized population standard deviation. No simulation result
is corrected. Evaluate MAE, RMSE, bias, Pearson and Spearman correlation. The
constant-mean volatility baseline is the endpoint's recorded `volatilityMean`
(0.00099081); its derivation is not independently certified as training-only.
This units mismatch limits claims about correct risk-scaled allocation, while
the saved execution remains reproducible and internally reconciled.

## Subgroups and uncertainty

Probability bins are fixed at [0,.1,.2,.25,.3,.4,.5,.75,1]. Volatility quartiles
are descriptive quantiles of all 41,595 logged forecasts; exact edges are saved.
No group is selected to rerun a strategy. Report observed ticker, probability,
volatility, interaction, decision, extension and zero-volume groups with counts
and labeled session counts. Empty/unobserved groups have no claimed estimates.
Probability acceptance (p>=.25), requested new allocation, and extension-only
decisions are separate concepts. Late entry rejection includes signals of any
probability. “Not allocated” is not equivalent to “below threshold.”

The classification AUC of predicted volatility itself diagnoses whether the
classifier merely ranks the chance of larger moves. This uses already-logged
scores and labels, does not fit a replacement model and does not trade on them.

Use 2,000 circular moving-block bootstrap resamples, with five adjacent sessions
per block, sampled until 59 sessions are obtained. Preserve all within-session
predictions/trades and cross-ticker dependence. Means and pooled ratios are
recomputed from session sums/counts. Trade groups use exit sessions, all trades
being intraday. AUC resampling uses integer observation weights and tied-rank
concordance; the classifier-versus-volatility AUC difference is paired under the
same resample. Brier comparison conditions on the original sample's prevalence
constant. Percentile 2.5/97.5 bounds are exploratory uncertainty intervals.

Only 59 sessions and one fixed universe are observed. Five-session blocks do not
guarantee all serial dependence is removed. No multiple-comparison correction,
external replication, formal significance claim or strategy-generalization claim
is made. Bucket rates based on 61 predictions or 2–3 trades are too unstable for
promotion. Extension groups are selected by subsequent signals, not randomized.

## Engineering denominators and timing scopes

Derived `cycles.jsonl` timing columns explicitly end in `_ms`; the original
artifact's fields end in `Nanos`. Conversion divides by one million.

- 4,571 market cycles; 2,773 inference cycles; 1,798 intentional empty-window
  morning cycles; 15 tickers per inference batch; 41,595 predictions; 545 trades.
- Batch prediction latency = `evaluation.predictionDurationNanos / 1e6` on
  `inferenceStatus == COMPLETED` only. Includes request mapping, local HTTP,
  Python feature/model work and response parsing. First inference stays included.
- Inference-cycle latency = `timing.totalNanos / 1e6` on the same 2,773 cycles.
  Morning cycle timing is a separate distribution and never lowers this latency.
- Batch latency/15 is **amortized milliseconds per ticker**, not standalone
  single-ticker response latency. No batch-size scaling claim: every batch is 15.
- Whole-run throughput = 41,595 / (`completion.fullBacktestWallNanos / 1e9`).
  Includes preparation, snapshot/provenance, execution and session serialization;
  excludes JVM startup, Python service startup, training, downloading and final
  completion-marker writing. Report driver and serialization separately.
- Stage totals sum logged non-overlapping exit, evaluation, entry/adjustment and
  valuation durations. Window, prediction and remaining decision timings are
  nested within evaluation and must **not** be added again to top-level totals.
- Per-cycle work groups compare entries/exits, with counts and latency
  distributions. Position/adjustment correlations are observational. No repeats,
  concurrent-load experiment, Python-internal profiler or speedup baseline exists.

All latency claims are one local CPU run on the exact hardware/software recorded
in the manifest. RAM capacity/maximum heap is not measured memory consumption.
Zero execution failures is observed run reliability, not a production uptime SLO.
