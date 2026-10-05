# Legacy versus corrected: serving-unit defect comparison

Engineering defect repair on the same predetermined evaluation; corrected run is authoritative regardless of financial outcome.

Legacy `4b966e0f-d9c1-4de3-8a09-5d4b5af44332`; canonical corrected `2ef02c28-189c-447a-af1e-880d15f9e575`. Returns are fractions and amounts are simulated dollars.

## Signal-path invariants

| check | result |
|---|---|
| session_equal | True |
| strategy_equal | True |
| endpoint_equal | True |
| calendar_equal | True |
| execution_assumptions_equal | True |
| snapshot_sha256_equal | True |
| model_identities_equal | True |
| cycle_timestamps_equal | True |
| market_cycles | 4571 |
| inference_cycles | 2773 |
| inference_pattern_equal | True |
| prediction_count | 41595 |
| classifier_probabilities_exactly_equal | True |
| classifier_probability_max_abs_difference | 0 |
| canonical_volatility_max_abs_difference | 2.30968e-10 |
| canonical_volatility_matches_float32_conversion | True |
| probability_eligibility_equal | True |
| normalized_decision_reasons_equal | True |
| extension_events_equal | True |
| extensions | 4550 |
| matched_entries | 545 |
| exit_times_equal | True |
| entry_prices_equal | True |
| exit_prices_equal | True |
| holding_periods_equal | True |
| per_trade_extensions_equal | True |
| trade_return_max_abs_difference | 0 |
| shared_trade_ids | 0 |
| trade_id_interpretation | UUIDs identify lifecycles within a run, not corresponding trades across runs; match ticker and entry timestamp. |

Eligibility normalizes CAPPED_ALLOCATION and ALLOCATION_REQUESTED to NEW_ENTRY. Cap status is an allocation result, not signal eligibility. Trade UUIDs differ across runs; ticker/entry-time matches are unique.

## Capital-allocation differences across the actual trajectories

| metric | value |
|---|---|
| comparison | Actual complete recorded trajectories, including all downstream cash effects; not the fixed-state counterfactual. |
| cash_tolerance | 1e-08 |
| quantity_tolerance | 1e-10 |
| entry_requests | 545 |
| changed_requests | 543 |
| changed_requests_over_one_cent | 543 |
| changed_quantities | 543 |
| entry_cycles | 370 |
| changed_entry_cycles | 368 |
| cycles_with_changed_total_deployment | 367 |
| reason_changes | 12 |
| capped_legacy | 339 |
| capped_corrected | 339 |
| legacy_total_entry_notional | 640721 |
| corrected_total_entry_notional | 636237 |
| equity_samples_changed | 4570 |
| max_abs_equity_difference | 27.2815 |
| terminal_equity_difference | -17.1951 |
| zero_price_return_trades | 2 |
| flat_price_cash_roundoff_max_abs | 2.84217e-14 |
| roundoff_note | Strict cash-P&L signs retain legacy methodology: corrected losses/flat are 258/1 versus 257/2. One unchanged-price trade has -2.842e-14 dollars of floating-point residue. Economically both runs have two flat-price trades; winning count is unchanged. |

Changed-request distributions (relative changes are fractions):

| metric | n | mean | min | median | p95 | max |
|---|---|---|---|---|---|---|
| changed_request_abs_dollars | 543 | 35.532 | 0.0241479 | 12.6655 | 163.203 | 350.372 |
| changed_request_relative_delta | 543 | -0.0167923 | -0.319818 | -0.00173719 | 0.103694 | 0.35148 |

These include changed cash after earlier trades. They must not be confused with the earlier 212/545 changed requests calculated while holding each legacy decision's cash fixed.

## Results and engineering timings

| metric | legacy | corrected | delta |
|---|---|---|---|
| total_return | 0.0180436 | 0.0163241 | -0.00171951 |
| gross_pnl | 180.436 | 163.241 | -17.1951 |
| max_drawdown | -0.0222202 | -0.0233636 | -0.00114343 |
| spy_total_return | 0.0229914 | 0.0229914 | 0 |
| win_rate | 0.524771 | 0.524771 | 0 |
| profit_factor | 1.12411 | 1.11083 | -0.0132779 |
| equity_turnover | 127.587 | 126.881 | -0.706117 |
| total_traded_notional | 1.28162e+06 | 1.27264e+06 | -8984.3 |
| linear_break_even_cost_bps | 1.40787 | 1.2827 | -0.125175 |
| max_single_ticker_equity_weight | 0.203435 | 0.203433 | -2.7995e-06 |
| max_top3_equity_weight | 0.514856 | 0.512762 | -0.00209352 |
| full_backtest_wall_seconds | 30.7423 | 27.295 | -3.44731 |
| predictions_per_full_wall_second | 1353.02 | 1523.91 | 170.884 |
| prediction_seconds | 28.0457 | 24.4961 | -3.54961 |
| mean_exposure | 0.215285 | 0.216518 | 0.00123371 |
| peak_exposure | 0.886665 | 0.887298 | 0.000633719 |
| mean_invested_hhi | 0.480267 | 0.483895 | 0.0036279 |
| prediction_ms_median | 9.9791 | 8.2659 | -1.7132 |
| prediction_ms_p95 | 11.7404 | 11.1303 | -0.61014 |
| prediction_ms_mean | 10.1138 | 8.83379 | -1.28006 |
| prediction_ms_max | 81.9599 | 50.0559 | -31.904 |

Two single runs on the recorded local CPU environments, not randomized repeated measurements. Observed differences cannot isolate conversion overhead or establish a performance regression/speedup.

## Recorded source changes

| path | legacy | corrected |
|---|---|---|
| algotrader/src/test/java/com/algotrader/logging/VolatilityUnitsIntegrationTest.java | None | 3fc1968f6acd6266740680c51ffca16a20b7256817333c3bd245eac7caad8d58 |
| analytics/analyze_showcase.py | None | 89b03a27eb77fa1b07fde4ac4c40507932ac88e3291035ae7a544be7c35b1c2c |
| analytics/tests/test_analytics.py | None | de8aaae043ed38417c32d667f0c993ee7483570321a8f386e3c91639e98ff849 |
| python-model/app/model_server.py | 67d2b6f2ea6b4d777fca62e000ad92a3ba0e9dbeb5c463613b4e916c948b03f8 | 252215878438499c4205a8ec4b7eef4230823efb2379cbb557e54a229d00e16e |
| python-model/app/volatility_units.py | None | 2779004de03039e1090e18b9b0f109c84ea7cef3a227a98ea7c0342fe2296a9f |
| python-model/tests/test_volatility_units.py | None | d12f999e426eec7cf3fe0d39e3b64a81dd60c944329d4854cd5ffe2cd3040821 |

## Cost sensitivity

| one_way_bps | legacy_pnl | corrected_pnl | delta |
|---|---|---|---|
| 1 | 52.2736 | 35.9769 | -16.2966 |
| 5 | -460.375 | -473.078 | -12.7029 |
| 10 | -1101.19 | -1109.4 | -8.21076 |

Fixed fills and notionals; no cost-induced capital feedback or strategy rerun.


## Ticker allocation and concentration

| ticker | trades | legacy_entry_notional | corrected_entry_notional | legacy_entry_share | corrected_entry_share | legacy_pnl | corrected_pnl | legacy_mean_equity_weight | corrected_mean_equity_weight |
|---|---|---|---|---|---|---|---|---|---|
| AAPL | 30 | 31199.3 | 30786.8 | 0.048694 | 0.0483889 | 113.959 | 112.923 | 0.00706272 | 0.00699323 |
| MSFT | 37 | 37569.2 | 36399.1 | 0.0586359 | 0.0572099 | -9.53396 | -12.1557 | 0.00839636 | 0.00813272 |
| NVDA | 45 | 47564.1 | 47101.3 | 0.0742353 | 0.074031 | 27.4273 | 28.3676 | 0.0183974 | 0.0184306 |
| AMD | 90 | 136382 | 138686 | 0.212857 | 0.217979 | 160.508 | 152.914 | 0.0641368 | 0.0667661 |
| GOOGL | 37 | 39375.6 | 38430.9 | 0.0614552 | 0.0604034 | -50.5328 | -51.5463 | 0.0120228 | 0.0117443 |
| AMZN | 38 | 31524.6 | 31168.3 | 0.0492018 | 0.0489884 | -31.2028 | -30.3164 | 0.00719781 | 0.00709783 |
| META | 78 | 89108.3 | 87896.7 | 0.139075 | 0.138151 | 53.3302 | 49.6068 | 0.0316403 | 0.0314984 |
| TSLA | 105 | 133550 | 132318 | 0.208437 | 0.207969 | -123.072 | -122.714 | 0.0424682 | 0.0420735 |
| JPM | 16 | 13448.1 | 13355.5 | 0.020989 | 0.0209914 | -0.250136 | -0.0780734 | 0.00356314 | 0.00350971 |
| V | 6 | 4558.54 | 4428.26 | 0.00711471 | 0.00696007 | 0.0118487 | 0.209812 | 0.00128351 | 0.00123518 |
| XOM | 29 | 38955.9 | 38776.9 | 0.0608001 | 0.0609473 | 27.4591 | 26.4185 | 0.0101961 | 0.0102153 |
| UNH | 18 | 23976.3 | 23671.4 | 0.0374208 | 0.0372053 | 15.6652 | 13.0215 | 0.00602675 | 0.00599265 |
| COST | 2 | 3137.59 | 3143.14 | 0.00489697 | 0.0049402 | 12.9122 | 12.9617 | 0.000840273 | 0.000841891 |
| SPY | 3 | 1331.62 | 1284.28 | 0.00207832 | 0.00201855 | 1.79892 | 1.75227 | 0.000229834 | 0.000221126 |
| QQQ | 11 | 9039.45 | 8790.3 | 0.0141082 | 0.0138161 | -18.0442 | -18.1247 | 0.00182255 | 0.0017657 |

## Defect-detection case study

A predetermined evaluation produced immutable run artifacts. Offline analytics compared model forecasts against their training targets and exposed a 1,000x serving-unit mismatch. Tracing target construction, checkpoint metadata, Python output, Java mapping and sizing identified the missing inverse transform. A shared metadata-driven serving conversion, versioned provenance and Python/Java regression tests repaired the contract without tuning the strategy. The same predetermined evaluation was then rerun.

The two completed runs retain 41,595 predictions, 545 matched entry/exit lifecycles and 4,550 extension events. Across the complete trajectories, 543 of 545 entry amounts and 543 quantities differ. Terminal marked equity differs by $-17.195058. The tables quantify exposure, turnover, drawdown and costs. These financial differences describe the repair's allocation consequences, not evidence that the strategy improved or deteriorated.

Reproducibility, observability and cross-language contract validation made the defect diagnosable and the correction verifiable. Discovering the defect alone is not an accomplishment claim.

See `comparison_allocations.jsonl` for matched trades and amounts, `comparison_equity.jsonl` for every paired post-cycle sample, and `comparison.json` for exact metrics. Input and output hashes are in `analysis_completion.json`.
