# Corrected fixed showcase analytics

Canonical evaluation: `2ef02c28-189c-447a-af1e-880d15f9e575`.
Legacy `4b966e0f-d9c1-4de3-8a09-5d4b5af44332` is used only for the explicitly
labeled before/after engineering comparison. Neither strategy is rerun.

## Regeneration

From the repository root, using the existing environment:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s analytics/tests -v
.\.venv\Scripts\python.exe analytics/analyze_corrected.py
```

Optional `--output reports/showcase/corrected-check` selects another derived
directory. The script rejects destinations outside `reports/showcase`, its root,
and the legacy report directory or any descendant. Default output is
`reports/showcase/2ef02c28-189c-447a-af1e-880d15f9e575/`. Regeneration replaces only
derived files there. No server, ingestion, training, strategy or executor calls
are made. Requirements are in `analytics/requirements.txt`.

The script shares extraction, portfolio arithmetic, benchmark, trade statistics,
subgroups, uncertainty procedures, charts and resume-claim ingredients with
`analyze_showcase.py`. Both runs are recalculated in memory using the same
formulas. The original legacy reports are not regenerated or overwritten.
All files in both run directories and the legacy report directory are hashed
before and after. Completion includes those hashes, both analysis source hashes
and hashes of all derived outputs. Target-building source hashes must match both
manifests; SQLite is opened read-only and immutable.

## Definitions retained and explicit differences

[METHODS.md](METHODS.md) remains the detailed legacy-method reference. Its equity,
benchmark, latency, turnover, cost sensitivity, target timing, block bootstrap,
sample restrictions and subgroup formulas also apply here. Legacy-specific unit
and fixed-run statements are superseded below for the corrected report.

- Post-cycle equity, including positions, is used consistently. Return is final
  equity / initial equity - 1; P&L is the sum of exit minus entry fill notionals.
- SPY buys at the frozen five-minute candle open July 7, 2026 16:05 UTC
  ($747.6699829101562) and sells at September 28 19:55 UTC
  ($764.8599853515625). Same initial $10,000, fractional shares and zero modeled
  transaction costs, spread, slippage or delay. Price-only return, no dividends.
  SPY holds overnight while the strategy is intraday; exposure is not matched.
- The corrected manifest records `volatility-return-fraction-v1`,
  `units=return_fraction`, `inverseTargetScalingApplied=true`. Its logged values
  are used unchanged: divisor **1**. The checkpoint's training scale remains 1000
  but does not imply another conversion. Only the identified legacy run is
  canonicalized from its checkpoint scale. Old float64 analytics conversion and
  new float32 serving conversion differ by at most approximately 2.31e-10;
  matching also tests exact equality to the float32 conversion.
- The threshold event is terminal 30-minute return >=0.002. Directional AUC uses
  return >0 (zero is negative); absolute-move AUC uses abs(return)>=0.002;
  negative-event AUC uses return<=-0.002. Conditional tail direction is evaluated
  only where abs(return)>=0.002. These use fixed-horizon market returns, not
  path-touch events or extended-trade outcomes.
- Classifier and directional AUC uncertainty use the same paired circular
  five-session-block bootstrap: 2,000 draws, seed 20260930. All tickers and
  overlapping horizons within a selected session are kept together. The methods
  do not eliminate cross-session dependence or provide multiple-comparison
  corrections. The 59-session sample does not establish future generalization.
- Cash P&L signs retain the original strict definition. One corrected trade has
  unchanged entry/exit price but -2.842170943040401e-14 dollars of rounding
  residue. Hence machine-readable cash signs show 286 wins, 258 losses, one
  exactly flat trade; price returns show two economically flat trades, as in
  legacy. This residue is disclosed rather than editing source or fills.

## Comparing the actual trajectories

Predictions join by cycle timestamp and ticker. Candidate/eligibility decisions
normalize `CAPPED_ALLOCATION` and `ALLOCATION_REQUESTED` to `NEW_ENTRY`; changes
between those two reasons reflect caps, not a changed signal path. Extensions
join cycle, ticker, entry timestamp, previous/new planned exits and reason.
Trade IDs are run-local UUIDs: pair trades by unique ticker/entry timestamp,
then verify exit times/prices, holding periods and extensions.

Entry fill notional equals requested cash under the verified executor.
`comparison_allocations.jsonl` records both requests/fills, quantities, IDs and
outcomes. Amount changes use absolute tolerance 1e-8 dollars; quantity changes
use 1e-10 shares; a separate count uses one cent. Each run uses its own recorded
cash trajectory, so downstream effects are included. This differs from the
previous fixed-state reconstruction with 212 changed requests. No alternative
allocation policy, period or parameter is simulated.

`comparison_equity.jsonl` includes every pair of post-cycle equity, exposure and
drawdown observations. The summary compares turnover, ticker entry-notional
shares, average ticker equity weights, HHI, exposure, final P&L, win rate, profit
factor and fixed-fill cost sensitivities. One-way costs are applied to all buy
and sell notional at 1, 5 and 10 bps without cost-induced capital feedback.

Engineering comparisons use each run's recorded timings and environments.
Prediction percentiles use only 2,773 actual 15-ticker requests, including first
inference. No intentional morning skips enter these distributions. Application
throughput is 41,595 divided by `completion.fullBacktestWallNanos / 1e9`, including
setup/snapshot, simulation and serialization, excluding JVM/Python startup,
training and downloads. Two single runs cannot isolate the serving conversion's
latency effect or justify a performance-regression/speedup claim.

## Output and limitations

The same 13 artifact types as legacy are produced: metrics, report, findings,
resume candidates, cycle/prediction/trade/session JSONL, four PNG charts and
analysis completion. Four additional files contain the engineering comparison:
`comparison.json`, `comparison.md`, `comparison_allocations.jsonl`, and
`comparison_equity.jsonl`. Claim category A includes calculations, denominators,
source fields, qualifications and justification; no final resume bullet is written.

Retained limitations: 59 sessions, fixed ticker universe, overlapping targets,
zero modeled costs/slippage, unmatched overnight SPY exposure, and 21 zero-volume
vendor candles in 420 input windows/372 cycles. Subgroups by ticker, probability,
volatility, their interaction, acceptance, allocation, extensions, exposure and
vendor anomalies are descriptive, not independently validated strategies or
recommendations to tune the strategy. Corrected units determine which evaluation
is canonical; its relative profitability does not.
