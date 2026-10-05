# Volatility serving contract

Both classification-with-volatility endpoints expose population standard
deviation (`ddof=0`) of six consecutive five-minute simple returns over the
forward 30-minute target window. Values are return fractions, not annualized.
This is not the standard deviation of the cumulative 30-minute return.

Training multiplies this target by checkpoint `target_scale`. Serving validates
that metadata is numeric, finite and positive at model loading, and reverses it
once in `app.volatility_units.predict_return_fraction_volatility`. Both routes
share this postprocessing; the network forward pass and checkpoint stay intact.
For raw output 1 and scale 1000, the API returns approximately 0.001 (float32
rounding applies). Java maps that value unchanged. No consumer divides again.

`/provenance` records `volatilityOutputContract`, version
`volatility-return-fraction-v1`, including the target definition, units and
`inverseTargetScalingApplied=true`. Model identity retains checkpoint
`target_scale`. Java includes the service provenance in each new run manifest.
The OpenAPI volatility field descriptions state the same contract.

The existing endpoint `outputStatistics.volatilityMean=0.00099081` and
`volatilityStd=0.00032641` in `batch-cnn-v1.json` and
`cnn-with-volatility-v1.json` are already in return-fraction units. They describe
the reference mean and standard deviation for volatility standardization (not
a second target scaling factor). Their numeric values are unchanged; their
original estimation dataset is not recorded, so this repair does not certify
them as training-only estimates. The contract declares their units as well.
The batch allocator does not consume these statistics; the single-ticker
planner's standardization requires the canonical units.

Run `4b966e0f-d9c1-4de3-8a09-5d4b5af44332` predates this contract and retains
scaled outputs. Neither it nor its derived reports is rewritten. Analytics
recognizes that exact legacy identity and fails closed for unidentified units.

## Validation without a backtest

From the repository root in PowerShell:

```powershell
$env:VOLATILITY_TEST_PYTHON = (Resolve-Path .venv/Scripts/python.exe).Path
$env:PYTHONPATH = (Resolve-Path python-model).Path
mvn -f algotrader/pom.xml clean package
.\.venv\Scripts\python.exe -m unittest discover -s python-model/tests -v
.\.venv\Scripts\python.exe -m unittest discover -s data-ingestion/tests -v
.\.venv\Scripts\python.exe -m unittest discover -s analytics/tests -v
java -jar algotrader/target/algotrader-1.0.0-SNAPSHOT.jar batch-cnn-v1 --validate-only
```

The Java cross-language test requires `VOLATILITY_TEST_PYTHON`; otherwise it is
explicitly skipped. It obtains serialized responses from the actual Python
routes using a known raw forecast, feeds those responses over HTTP through both
Java providers, then checks the unchanged production allocation formula.
Python also checks the real loaded checkpoint and both HTTP routes.

Restart the Python service before a corrected run so its process loads the new
postprocessing and reports this contract. Coverage preflight does not start a
backtest or contact the model server. No strategy parameters, date bounds,
inputs, weights or execution assumptions are changed by this repair.

## Repair validation, October 5, 2026

Clean compilation/package and all 30 Java tests passed, including the Python to
Java integration test (none skipped). All 11 model-server, 5 ingestion and 7
analytics tests passed. Validation used the installed Python 3.12 interpreter
with the project's virtual-environment packages; Maven used an offline copy of
the existing dependency cache. No dependencies or project build settings changed.

Coverage preflight passed for the unchanged inclusive execution interval
`2026-07-07T16:05:00Z` through `2026-09-28T19:55:00Z`: 4,571 market cycles,
2,773 full-universe inference-capable cycles and 1,798 intentional warm-up cycles.
All 15 tickers retain the required coverage. The previously disclosed 21
zero-volume candles remain a vendor-data limitation.

Before/after SHA-256 checks matched all 192 protected files across configuration,
model checkpoints, data/run artifacts, existing reports and production Java.
The classifier hash remains
`ff505dc687735111e4913774cba998ef70a8b7801474739409adf00af87729f4`;
the volatility model hash remains
`c7a14302c33c59ea8b0daa9169cfc2f66c313b9568d7a792c77ed048cfd9112f`.

The cross-language example uses two tickers with raw volatility 1 and probability
0.5. Scale 1000 yields API volatility approximately 0.001; both Java providers
retain it. With cash 1000, threshold 0.25, deployment fraction 0.4 and cap 0.2,
each score is approximately `0.25 / 1.001 = 0.24975025`; equal weights request
200 per ticker. Assertions check the score as well as the capped allocation, so
caps cannot mask a regression back to training units. No showcase backtest was
launched and no existing analytics output was regenerated.
