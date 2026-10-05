# Generating an analysis-ready historical run

Run these commands from the repository root. The Java entry point defaults to
`config/session/batch-cnn-v1.json`; the optional argument selects another session
file by ID. This instrumentation targets the historical multi-ticker
classification-with-volatility workflow.

## Commands

Restart the Python server with the updated code. In the first PowerShell terminal:

```powershell
Set-Location C:\Users\danzo\OneDrive\Desktop\algo-trader
.\.venv\Scripts\python.exe -m uvicorn --app-dir python-model app.model_server:app --host 127.0.0.1 --port 8000
```

Use one server process without `--reload`. The service now reports the hashes of
the exact checkpoint bytes it loaded. Java checks its instance ID on every batch
response and fails the run if the server changes. Model files must already exist
in `python-model/saved_models`; this command does not train or change them.

In a second PowerShell terminal:

```powershell
Set-Location C:\Users\danzo\OneDrive\Desktop\algo-trader
mvn -f algotrader/pom.xml clean package
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
java -jar algotrader/target/algotrader-1.0.0-SNAPSHOT.jar batch-cnn-v1
if ($LASTEXITCODE -ne 0) { throw 'Backtest failed; inspect the printed run directory' }
```

Review the session's dates/tickers before running. The current config requests
2026-07-07T16:05:00Z through 2026-09-28T19:55:00Z, inclusive execution times.
The command uses the local
`data/ohlcv.db` and does not download market data. Each run takes a consistent
SQLite snapshot (including committed WAL contents) and reads that snapshot.
Allow disk space for one additional database copy per run. Updating the original
database afterward does not change the run's inputs.

To check data coverage without starting the model server, creating run artifacts,
performing inference, or running a backtest, use the built jar:

```powershell
java -jar algotrader/target/algotrader-1.0.0-SNAPSHOT.jar batch-cnn-v1 --validate-only
```

Every real run repeats this check against its frozen snapshot before simulation.
It rejects out-of-range requests, non-executable/off-grid bounds, missing execution
candles (including the final liquidation price), invalid prices, and incomplete
input windows. The first cycle must have the endpoint's full completed lookback.
Later mornings may have empty windows until enough consecutive same-day candles
have closed. No overnight candles are invented or stitched into the model input.

## Refreshed data and chosen interval (September 29, 2026)

The refresh used the existing downloader with explicit full-retention settings:

```powershell
.\.venv\Scripts\python.exe -u data-ingestion/update_ohlcv.py --interval 5m --full-history --session config/session/batch-cnn-v1.json --backup
```

The downloader now normalizes pandas datetime resolution explicitly to Unix
seconds, rejects implausible/off-grid/duplicate timestamps, and excludes forming
candles. This refresh requested yfinance `period=60d`, `interval=5m`,
`auto_adjust=False`, `prepost=False`. Its actual response covered July 7 through
September 29 at 19:20 UTC: 4,673 completed candles per ticker, including 71 candles
in the still-incomplete September 29 session. The response spans 60 market dates;
do not interpret `60d` as a guarantee of exactly 60 calendar dates.

All 15 tickers have identical timestamp coverage. Each has 10,427 genuine
five-minute candles overall, from February 19 at 14:30 UTC through September 29
at 19:20 UTC. July 7–September 28 provides 59 consecutive complete market
sessions, 4,602 candles per ticker, with no missing regular-session slots and
finite, positive OHLC prices. However, 21 candles across 14 sessions report zero
volume despite non-flat OHLC prices. All 21 still reported zero on a second
yfinance download. Their values are preserved without imputation; the models'
existing `log1p(volume)` input accepts zero, so ordinary coverage validation alone
does not reject these anomalies.

The user chose to retain **all 59 timestamp-complete sessions and disclose these
volume anomalies**. The selected interval therefore retains the vendor's original
values; it must not be described as free of data-quality concerns. For reference,
excluding every affected session would leave July 21–August 3 as the longest
contiguous block (10 complete sessions, 780 candles per ticker). That restriction
is not applied. No profitability results were used to select either interval.

The older February 19–June 3 block has 73 complete sessions,
but overlaps model development and is not the chosen recent evaluation block.

Both checkpoint files still match their recorded provenance hashes. Both report
training cutoff May 1 at 19:40 UTC and validation cutoff May 18 at 19:45 UTC.
Even the first input candle, July 7 at 13:30 UTC, is well after those cutoffs.
Selection uses only coverage, calendar, input/execution semantics, and cutoffs.

The first execution is July 7 at 16:05 UTC (12:05 Eastern), after 31 completed
five-minute input candles beginning at 09:30 Eastern. The final execution is
September 28 at 19:55 UTC (15:55 Eastern), with an exact candle-open price for
liquidation. Expected workload: **4,571 market cycles, 2,773 full-universe
inference-capable cycles, and 1,798 morning warm-up cycles**. Each later full
session has 31 warm-up cycles and 47 inference-capable cycles. These counts
describe available inputs, not guaranteed successful model-service responses.

Remaining coverage limitations are outside the selected interval: June 4 lacks
its last 18 bars; June 5–July 6 has 20 wholly missing sessions (1,560 bars per
ticker); September 29 lacks its final seven bars in this download. Those recent
end-of-day bars were still forming or in the future at retrieval time. No bars
were fabricated. The 30 confirmed timestamp-1 rows (15 per interval) were removed
in a targeted cleanup; no timestamp-1 rows remain. All genuine older history and
unrelated data were preserved. The complete pre-refresh database, including the
bad rows, is retained at `data/backups/ohlcv-20260929T192626Z-f903c75a.db`.

See [the coverage record](showcase-data-coverage-2026-09-29.json) for per-ticker
counts, every incomplete session, zero-volume candles and clean session blocks,
model hashes/cutoffs, calendar source, and
database checksum. That checksum identifies this refresh, not future downloads.
No showcase backtest was run during this repair.

## Output

The program prints `data/logs/runs/<UUID>/`. Every run has a separate directory;
`latest-session.json` is no longer overwritten.

| File | Contents |
| --- | --- |
| `manifest.json` | Schema `algotrader-run-v1`, run ID, resolved session/endpoint/strategy/calendar settings, historical range, validated coverage and expected cycle/window counts, tickers, Git commit/dirty state, source-file and Java build hashes, database snapshot hash, model-service identity and both checkpoint hashes, available training/validation cutoffs, runtime details, execution assumptions |
| `ohlcv.db` | The consistent SQLite snapshot actually used by this run |
| `session.json` | Initial/final marked portfolios, every cycle's state, predictions/decisions, fills, failed attempts, extensions, timing, liquidation result, remaining open trades, and failures |
| `completion.json` | Completion status, monotonic wall-clock durations, cycle count, and SHA-256 of `session.json` |

A setup failure produces a completion marker with `SETUP_FAILED`; some other
files may not exist. An absent completion marker means the artifact is unfinished.
Session status is `FAILED`, `COMPLETED`, or `COMPLETED_WITH_OPEN_POSITIONS`.
`COMPLETED` means the loop finished and the portfolio is flat, not that every
execution attempt succeeded. Inspect failed attempts and valuation quality too.

Configuration credentials are excluded: endpoint user-info/query/fragment,
arbitrary strategy parameters, free-text descriptions, environment dumps, and
Git remote URLs are not logged. The server exports a fixed metadata allowlist.
Source files are hashed, not copied into the manifest. Keep the corresponding
checkout, build, and checkpoint files if you want to rerun the exact implementation;
hashes identify those files but do not recreate them.

## Reading the records

### Valuation

`cycleLogs[].portfolioAfterCycle` is sampled **after exits, evaluation, entries,
and applied exit extensions**, at the cycle timestamp (`POST_CYCLE`). It records
cash and, per holding, quantity, price, price timestamp, position value and mark
status. Equity is cash plus marked holdings. The existing snapshot inside
`evaluation` is the decision input, after exits and before entries.

Marks use the price provider's candle-open convention. Missing exact prices use
the last price observed by the valuator, including fills, with `STALE` status and
the original price timestamp. There is no interpolation or future-price lookup.
If no usable prior price exists, that position's price/value and total equity are
null (`INCOMPLETE`). Flat portfolios have equity equal to cash. A stale equity
estimate must not be reported as a fresh market valuation.

`finalPortfolio` is sampled after the final liquidation attempt. Liquidation uses
the final visited cycle timestamp, never an earlier historical price. Missing
execution prices produce failed attempts and retained holdings. This corrects the
old possibility of liquidating retroactively at an earlier market boundary.
An exceptional session stops without inventing liquidation fills; its final
snapshot has `AFTER_FAILURE` status. Partial cycles are retained and labeled
`PARTIAL_CYCLE_AFTER_FAILURE`.

### Timing and workload

All new duration fields are nanoseconds from `System.nanoTime()`:

- `timing.totalNanos`: full cycle work through portfolio valuation, including
  exits, inference/decisions, entries and adjustments; excludes final session
  serialization and the in-memory logger append.
- `timing`: separate exit, evaluation, entry/adjustment and valuation durations.
- `evaluation`: window retrieval, prediction round trip (mapping, HTTP, Python
  work, response parsing), remaining decision work, and total evaluation time.
- `completion.fullBacktestWallNanos`: configuration/provenance collection,
  database snapshot, initialization, cycles, liquidation and session serialization.
  Excludes JVM startup and writing/preparing the final completion marker.
- `completion.driverNanos` and `sessionWriteNanos`: execution and serialization
  separately. Session `startTime`/`endTime` are real wall times; historical bounds
  are explicit in the manifest, and historical sampling times accompany valuations.

For successful evaluations, `usableTickers` identifies valid windows,
`inferenceBatchSize` is the actual number of ticker inputs sent, and
`inferenceStatus` distinguishes `COMPLETED` from `SKIPPED_EMPTY_WINDOWS`.
Compare the manifest universe with `usableTickers` to identify missing tickers and
partial batches. Invalid windows are omitted by the existing provider. The clock
now uses the same market calendar and last-executable-time policy as trade exits,
including holidays and early closes. It emits no closed-market cycles. Exclude
morning warm-up cycles from inference latency claims; the first actual inference
remains visible.
An evaluation exception leaves evaluation null and records the cycle's failure
stage; do not include failed cycles in successful-inference latency percentiles.

### Predictions, decisions and trades

`evaluation.signals` retains probability, volatility, forecast horizon, applicable
score, decision reason and requested cash for every returned prediction, including
rejections and extension-only signals. Requested cash is after allocation caps;
successful entry fills contain the actual cash exchanged. Scores are null when
the strategy did not need to calculate a score. Existing allocation and adjustment
records remain the authoritative requested actions.

Trade IDs join opened trades, successful fills, failed attempts, extensions and
forced liquidation. Extensions retain the original trade ID and entry timestamp.
Failed entries also receive an ID but have no corresponding opened trade.
Distinguish repeated failed exits for one ID from distinct failed trades. On a
fatal portfolio/registry application failure, a successful execution record may
precede the failure: reconcile it against the preserved final state before using
that partial run.

## Analytics supported later

- Equity/return curves, drawdowns, exposure, concentration and time invested,
  with stale/incomplete mark filtering.
- Completed-trade realized P&L, holding times, win/loss distributions, turnover,
  ticker attribution, exit extensions, failed attempts and liquidation effects.
- Probability calibration and threshold outcomes by joining predictions to the
  frozen OHLCV data. Fixed-horizon model targets and extended trade returns are
  separate quantities.
- Batch-size-aware latency percentiles, ticker predictions per second, stage
  costs and full-run throughput, with skipped/failed work distinguished.
- Reproducibility and coverage checks, and later benchmark comparisons using the
  stored historical interval and data.

## Deliberate limits

1. **Valuation:** no fee/slippage/spread/dividend/corporate-action accounting and
   no interpolation across gaps. Marks are at cycle frequency, not tick-level
   intracycle highs/lows. These remain explicit execution/data assumptions.
2. **Provenance:** no model retraining or bundle of model/source binaries. Git,
   source/build hashes and actual loaded checkpoint hashes identify them. The
   snapshot preserves data, but the existing database has no per-row vendor or
   download lineage to recover. Keep the exact source/checkpoints separately.
3. **Timing:** no Python-internal stage profiler, memory profiler, automatic
   warm-up removal, or per-ticker missing-candle diagnosis. Failed evaluations
   retain failure/stage timing but not a partially completed prediction batch.
4. **Decisions:** no counterfactual strategy reruns, future outcome labels,
   calibration scores, or optimization during the backtest. Those are derived
   afterward from the raw predictions and data.
5. **Lifecycle:** no broker order/partial-fill system or crash-resume journal.
   Existing paper fills plus trade IDs cover this executor. Logs are written at
   session completion (including handled exceptions); a killed process can lose
   in-memory cycle records, and must not be presented as a completed run.

No charts, metric selection, allocation-policy changes, model/input changes, or
analytics database ingestion are included in this repair.

## Focused validation

```powershell
mvn -f algotrader/pom.xml clean compile
mvn -f algotrader/pom.xml test
.\.venv\Scripts\python.exe -m unittest discover -s data-ingestion/tests -v
Push-Location python-model
try { ..\.venv\Scripts\python.exe -m unittest discover -s tests -v }
finally { Pop-Location }
```

The Java tests cover synthetic runs, calendar boundaries and coverage failures
using temporary databases. Ingestion tests cover all four pandas timestamp
resolutions, distinct five-minute rows, invalid timestamps, forming candles, and
backup/upsert preservation. Model-server Python tests
load the existing model checkpoints and exercise small inference requests. No
full historical showcase backtest is needed for these checks.

September 29 repair validation: Java clean compilation and packaged build passed;
all 29 Java tests, 5 ingestion tests, and 7 model-server tests passed. The packaged
jar's `--validate-only` command passed against the refreshed database and selected
July 7–September 28 configuration, reporting 4,571 / 2,773 / 1,798 cycles as above.
SQLite integrity passed. A row comparison with the backup confirmed that all
original genuine rows remained unchanged; 70,095 genuine rows were added.
