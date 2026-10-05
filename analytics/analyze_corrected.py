"""Offline analytics for the predetermined corrected showcase; never runs a strategy."""
import argparse
import json
import platform
from pathlib import Path

import numpy as np
import pandas as pd
import analyze_showcase as base

RUN_ID = base.CORRECTED_RUN_ID
LEGACY_ID = base.RUN_ID
ROOT = base.ROOT


def hashes(directory):
    return {str(p.relative_to(directory)): base.sha(p) for p in sorted(directory.rglob("*")) if p.is_file()}


def validate_output(out):
    out = out.resolve()
    reports = (ROOT / "reports/showcase").resolve()
    legacy = reports / LEGACY_ID
    if reports not in out.parents or out == legacy or legacy in out.parents or out in legacy.parents:
        raise ValueError("Output must be below reports/showcase and separate from legacy reports")
    return out


def jsonl(path, frame):
    with path.open("w", encoding="utf-8") as stream:
        for row in frame.to_dict("records"):
            stream.write(json.dumps(base.clean(row), allow_nan=False) + "\n")


def model_diagnostics(metrics, predictions):
    valid = predictions.dropna(subset=["target_return"])
    result = {}
    for name, labels in {
        "positive_direction": valid.target_return > 0,
        "negative_threshold": valid.target_return <= -.002,
        "absolute_move": valid.target_return.abs() >= .002,
    }.items():
        result[name] = dict(n=len(valid), positive_count=int(labels.sum()),
            classifier_auc=base.auc(labels, valid.probability),
            volatility_score_auc=base.auc(labels, valid.volatility_return_units))
    result["positive_direction"]["session_block_uncertainty"] = base.auc_block_interval(
        (valid.target_return > 0).astype(int).to_numpy(), valid.probability.to_numpy(),
        valid.day.to_numpy(), valid.volatility_return_units.to_numpy())
    tails = valid[valid.target_return.abs() >= .002]
    result["direction_given_large_move"] = dict(n=len(tails),
        classifier_auc=base.auc(tails.target_return > 0, tails.probability),
        volatility_score_auc=base.auc(tails.target_return > 0, tails.volatility_return_units))
    result["probability_signed_return_spearman"] = base.correlation(valid.probability, valid.target_return, True)
    result["probability_realized_volatility_spearman"] = base.correlation(valid.probability, valid.realized_volatility, True)
    result["interpretation"] = "Fixed-horizon terminal returns, not path-touch events or extended-trade outcomes; exploratory diagnostics with overlapping targets."
    metrics["prediction"]["movement_diagnostics"] = result
    metrics["tables"]["movement_by_volatility_quartile"] = base.grouped(valid, "volatility_bucket", lambda f: dict(
        labeled=len(f), positive_threshold_rate=(f.target_return >= .002).mean(),
        negative_threshold_rate=(f.target_return <= -.002).mean(), positive_direction_rate=(f.target_return > 0).mean(),
        mean_fixed_target_return=f.target_return.mean(), classifier_auc=base.auc(f.label, f.probability)))


def matched_trades(legacy, corrected):
    """Trade UUIDs are run-local. Match unique ticker/entry-time events instead."""
    keys = ["ticker", "entry_time"]
    joined = legacy.merge(corrected, on=keys, suffixes=("_legacy", "_corrected"), how="outer",
                          validate="one_to_one", indicator=True)
    if not joined._merge.eq("both").all():
        raise ValueError("Entry event structures differ; a matched-trajectory comparison is not valid")
    return joined.drop(columns="_merge")


def compare(old, new, lm, cm):
    om, _, os, _, oc, op, ot = old
    nm, _, ns, _, nc, npred, nt = new
    left = op.set_index(["stamp", "ticker"]).sort_index()
    right = npred.set_index(["stamp", "ticker"]).sort_index()
    assert left.index.equals(right.index)
    joined = matched_trades(ot, nt)
    assert oc.stamp.tolist() == nc.stamp.tolist()
    def extension_events(session):
        return sorted((c["cycleTime"], a["targetTrade"]["ticker"], a["targetTrade"]["entryTime"],
                       a["targetTrade"]["exitTime"], a["newExitTime"], a["reason"])
                      for c in session["cycleLogs"] for a in c["appliedOpenTradeAdjustments"])
    def eligibility(reason):
        return reason.replace({"CAPPED_ALLOCATION": "NEW_ENTRY", "ALLOCATION_REQUESTED": "NEW_ENTRY"})
    probability_delta = right.probability.to_numpy() - left.probability.to_numpy()
    vol_delta = right.volatility_return_units.to_numpy() - left.volatility_return_units.to_numpy()
    scale = base.volatility_divisor(om)
    float32_expected = (left.volatility.to_numpy() / scale).astype(np.float32).astype(float)
    invariant = dict(
        session_equal=om["session"] == nm["session"], strategy_equal=om["strategy"] == nm["strategy"],
        endpoint_equal=om["endpoint"] == nm["endpoint"], calendar_equal=om["marketCalendar"] == nm["marketCalendar"],
        execution_assumptions_equal=om["assumptions"] == nm["assumptions"],
        snapshot_sha256_equal=om["data"]["sha256"] == nm["data"]["sha256"],
        model_identities_equal=om["predictionService"]["models"] == nm["predictionService"]["models"],
        cycle_timestamps_equal=True, market_cycles=len(nc), inference_cycles=int(nc.inference.sum()),
        inference_pattern_equal=oc.inference.equals(nc.inference), prediction_count=len(right),
        classifier_probabilities_exactly_equal=bool(np.array_equal(left.probability, right.probability)),
        classifier_probability_max_abs_difference=float(np.max(np.abs(probability_delta))),
        canonical_volatility_max_abs_difference=float(np.max(np.abs(vol_delta))),
        canonical_volatility_matches_float32_conversion=bool(np.array_equal(float32_expected, right.volatility_return_units)),
        probability_eligibility_equal=left.passes_probability.equals(right.passes_probability),
        normalized_decision_reasons_equal=eligibility(left.reason).equals(eligibility(right.reason)),
        extension_events_equal=extension_events(os) == extension_events(ns), extensions=len(extension_events(ns)),
        matched_entries=len(joined), exit_times_equal=bool(joined.exit_time_legacy.eq(joined.exit_time_corrected).all()),
        entry_prices_equal=bool(joined.entry_price_legacy.eq(joined.entry_price_corrected).all()),
        exit_prices_equal=bool(joined.exit_price_legacy.eq(joined.exit_price_corrected).all()),
        holding_periods_equal=bool(joined.holding_minutes_legacy.eq(joined.holding_minutes_corrected).all()),
        per_trade_extensions_equal=bool(joined.extensions_legacy.eq(joined.extensions_corrected).all()),
        trade_return_max_abs_difference=float((joined.trade_return_corrected-joined.trade_return_legacy).abs().max()),
        shared_trade_ids=len(set(ot.trade_id) & set(nt.trade_id)),
        trade_id_interpretation="UUIDs identify lifecycles within a run, not corresponding trades across runs; match ticker and entry timestamp.")
    for key in ["session_equal", "strategy_equal", "endpoint_equal", "calendar_equal", "execution_assumptions_equal",
                "snapshot_sha256_equal", "model_identities_equal", "inference_pattern_equal", "probability_eligibility_equal",
                "normalized_decision_reasons_equal", "extension_events_equal", "exit_times_equal", "holding_periods_equal"]:
        assert invariant[key], key
    joined["requested_cash_delta"] = joined.entry_notional_corrected - joined.entry_notional_legacy
    joined["requested_cash_relative_delta"] = joined.requested_cash_delta / joined.entry_notional_legacy
    joined["quantity_delta"] = joined.quantity_corrected - joined.quantity_legacy
    joined["allocation_changed"] = joined.requested_cash_delta.abs() > 1e-8
    requested = joined.groupby("entry_time")[["entry_notional_legacy", "entry_notional_corrected"]].sum()
    changed = joined[joined.allocation_changed]
    capital = dict(comparison="Actual complete recorded trajectories, including all downstream cash effects; not the fixed-state counterfactual.",
        cash_tolerance=1e-8, quantity_tolerance=1e-10, entry_requests=len(joined),
        changed_requests=int(joined.allocation_changed.sum()), changed_requests_over_one_cent=int((joined.requested_cash_delta.abs()>.01).sum()),
        changed_quantities=int((joined.quantity_delta.abs()>1e-10).sum()),
        entry_cycles=len(requested), changed_entry_cycles=int(joined.groupby("entry_time").allocation_changed.any().sum()),
        cycles_with_changed_total_deployment=int(((requested.entry_notional_corrected-requested.entry_notional_legacy).abs()>1e-8).sum()),
        changed_request_abs_dollars=base.distribution(changed.requested_cash_delta.abs()),
        changed_request_relative_delta=base.distribution(changed.requested_cash_relative_delta),
        reason_changes=int((left.reason != right.reason).sum()),
        capped_legacy=int((left.reason=="CAPPED_ALLOCATION").sum()), capped_corrected=int((right.reason=="CAPPED_ALLOCATION").sum()),
        legacy_total_entry_notional=ot.entry_notional.sum(), corrected_total_entry_notional=nt.entry_notional.sum())
    path = pd.DataFrame(dict(time=nc.time, equity_legacy=oc.equity, equity_corrected=nc.equity,
                            exposure_legacy=oc.exposure, exposure_corrected=nc.exposure,
                            drawdown_legacy=oc.drawdown, drawdown_corrected=nc.drawdown))
    path["equity_delta"] = path.equity_corrected-path.equity_legacy
    capital["equity_samples_changed"] = int((path.equity_delta.abs()>1e-8).sum())
    capital["max_abs_equity_difference"] = path.equity_delta.abs().max()
    capital["terminal_equity_difference"] = path.equity_delta.iloc[-1]
    flat = joined[joined.trade_return_corrected == 0]
    capital["zero_price_return_trades"] = len(flat)
    capital["flat_price_cash_roundoff_max_abs"] = flat.pnl_corrected.abs().max()
    capital["roundoff_note"] = "Strict cash-P&L signs retain legacy methodology: corrected losses/flat are 258/1 versus 257/2. One unchanged-price trade has -2.842e-14 dollars of floating-point residue. Economically both runs have two flat-price trades; winning count is unchanged."
    metrics = []
    for section, fields in {"financial": ["total_return", "gross_pnl", "max_drawdown", "spy_total_return"],
            "trades": ["win_rate", "profit_factor"],
            "risk": ["equity_turnover", "total_traded_notional", "linear_break_even_cost_bps", "max_single_ticker_equity_weight", "max_top3_equity_weight"],
            "engineering": ["full_backtest_wall_seconds", "predictions_per_full_wall_second", "prediction_seconds"]}.items():
        for field in fields:
            l, n = lm[section][field], cm[section][field]
            metrics.append(dict(metric=field, legacy=l, corrected=n, delta=n-l))
    for field, l, n in [("mean_exposure", lm["risk"]["exposure"]["mean"], cm["risk"]["exposure"]["mean"]),
            ("peak_exposure", lm["risk"]["exposure"]["max"], cm["risk"]["exposure"]["max"]),
            ("mean_invested_hhi", lm["risk"]["invested_hhi"]["mean"], cm["risk"]["invested_hhi"]["mean"])]:
        metrics.append(dict(metric=field, legacy=l, corrected=n, delta=n-l))
    for percentile in ["median", "p95", "mean", "max"]:
        l, n = [m["engineering"]["prediction_roundtrip_batch_ms"][percentile] for m in [lm, cm]]
        metrics.append(dict(metric="prediction_ms_"+percentile, legacy=l, corrected=n, delta=n-l))
    costs = [dict(one_way_bps=l["one_way_bps"], legacy_pnl=l["hypothetical_pnl"], corrected_pnl=n["hypothetical_pnl"],
                  delta=n["hypothetical_pnl"]-l["hypothetical_pnl"])
             for l,n in zip(lm["risk"]["cost_sensitivity_fixed_fills"],cm["risk"]["cost_sensitivity_fixed_fills"])]
    ticker_rows=[]
    for ticker in nm["session"]["tickers"]:
        l,n=ot[ot.ticker==ticker],nt[nt.ticker==ticker]
        ticker_rows.append(dict(ticker=ticker,trades=len(n), legacy_entry_notional=l.entry_notional.sum(),
            corrected_entry_notional=n.entry_notional.sum(), legacy_entry_share=l.entry_notional.sum()/ot.entry_notional.sum(),
            corrected_entry_share=n.entry_notional.sum()/nt.entry_notional.sum(), legacy_pnl=l.pnl.sum(),corrected_pnl=n.pnl.sum(),
            legacy_mean_equity_weight=oc["weight_"+ticker].mean(),corrected_mean_equity_weight=nc["weight_"+ticker].mean()))
    comp=dict(schema="showcase-unit-repair-comparison-v1", legacy_run_id=LEGACY_ID, corrected_run_id=RUN_ID,
        purpose="Engineering defect repair on the same predetermined evaluation; corrected run is authoritative regardless of financial outcome.",
        signal_path_invariants=invariant, capital_allocation_differences=capital, metrics=metrics,
        cost_sensitivity_fixed_fills=costs, ticker_allocations=ticker_rows,
        recorded_source_hash_differences=[dict(path=k,legacy=om["git"]["sourceFileSha256"].get(k),corrected=nm["git"]["sourceFileSha256"].get(k))
            for k in sorted(set(om["git"]["sourceFileSha256"]) | set(nm["git"]["sourceFileSha256"]))
            if om["git"]["sourceFileSha256"].get(k)!=nm["git"]["sourceFileSha256"].get(k)],
        latency_qualification="Two single runs on the recorded local CPU environments, not randomized repeated measurements. Observed differences cannot isolate conversion overhead or establish a performance regression/speedup.")
    return comp,joined,path


def write_comparison(out, comp):
    inv, cap = comp["signal_path_invariants"],comp["capital_allocation_differences"]
    text=["# Legacy versus corrected: serving-unit defect comparison\n",comp["purpose"],
          f"\nLegacy `{LEGACY_ID}`; canonical corrected `{RUN_ID}`. Returns are fractions and amounts are simulated dollars.\n",
          "## Signal-path invariants\n",base.md_table([dict(check=k,result=v) for k,v in inv.items()],["check","result"]),
          "\nEligibility normalizes CAPPED_ALLOCATION and ALLOCATION_REQUESTED to NEW_ENTRY. Cap status is an allocation result, not signal eligibility. Trade UUIDs differ across runs; ticker/entry-time matches are unique.\n",
          "## Capital-allocation differences across the actual trajectories\n",base.md_table([dict(metric=k,value=v) for k,v in cap.items() if not isinstance(v,dict)],["metric","value"]),
          "\nChanged-request distributions (relative changes are fractions):\n",base.md_table([dict(metric=k,**v) for k,v in cap.items() if isinstance(v,dict)],["metric","n","mean","min","median","p95","max"]),
          "\nThese include changed cash after earlier trades. They must not be confused with the earlier 212/545 changed requests calculated while holding each legacy decision's cash fixed.\n",
          "## Results and engineering timings\n",base.md_table(comp["metrics"],["metric","legacy","corrected","delta"]),
          "\n"+comp["latency_qualification"],
          "\n## Recorded source changes\n",base.md_table(comp["recorded_source_hash_differences"],["path","legacy","corrected"]),
          "\n## Cost sensitivity\n",base.md_table(comp["cost_sensitivity_fixed_fills"],["one_way_bps","legacy_pnl","corrected_pnl","delta"]),
          "\nFixed fills and notionals; no cost-induced capital feedback or strategy rerun.\n",
          "\n## Ticker allocation and concentration\n",base.md_table(comp["ticker_allocations"],list(comp["ticker_allocations"][0])),
          "\n## Defect-detection case study\n",
          "A predetermined evaluation produced immutable run artifacts. Offline analytics compared model forecasts against their training targets and exposed a 1,000x serving-unit mismatch. Tracing target construction, checkpoint metadata, Python output, Java mapping and sizing identified the missing inverse transform. A shared metadata-driven serving conversion, versioned provenance and Python/Java regression tests repaired the contract without tuning the strategy. The same predetermined evaluation was then rerun.\n",
          f"The two completed runs retain {inv['prediction_count']:,} predictions, {inv['matched_entries']} matched entry/exit lifecycles and {inv['extensions']:,} extension events. Across the complete trajectories, {cap['changed_requests']} of {cap['entry_requests']} entry amounts and {cap['changed_quantities']} quantities differ. Terminal marked equity differs by ${cap['terminal_equity_difference']:.6f}. The tables quantify exposure, turnover, drawdown and costs. These financial differences describe the repair's allocation consequences, not evidence that the strategy improved or deteriorated.\n",
          "Reproducibility, observability and cross-language contract validation made the defect diagnosable and the correction verifiable. Discovering the defect alone is not an accomplishment claim.\n",
          "See `comparison_allocations.jsonl` for matched trades and amounts, `comparison_equity.jsonl` for every paired post-cycle sample, and `comparison.json` for exact metrics. Input and output hashes are in `analysis_completion.json`.\n"]
    (out/"comparison.md").write_text("\n".join(text),encoding="utf-8")


def write_findings(out,m,comp):
    f,e,p,r,t = [m[k] for k in ["financial","engineering","prediction","risk","trades"]]
    facts=base.resume_facts(m)
    base.write_json(out/"resume_candidates.json",facts)
    lines=[f"# Corrected showcase findings\n\nCanonical run `{RUN_ID}`. The legacy run is used only for the labeled engineering comparison. No strategy selection or parameter tuning.\n",
           "## A. Strong resume candidates\n\nClaim ingredients, not final resume bullets.\n"]
    for fact in facts:
        lines += ["### "+fact["metric"]+"\n"]+[f"- {k}: {json.dumps(base.clean(v)) if isinstance(v,(dict,list)) else v}" for k,v in fact.items() if k!="metric"]
    move=p["movement_diagnostics"];auc=p["auc_session_block_uncertainty"]
    lines += ["\n## B. README / portfolio discussion\n",
        f"- Gross return {100*f['total_return']:.4f}% (${f['gross_pnl']:.4f}) versus SPY {100*f['spy_total_return']:.4f}%. Win rate {t['wins']}/{t['trades']} ({100*t['win_rate']:.3f}%); profit factor {t['profit_factor']:.4f}. Maximum drawdown {100*f['max_drawdown']:.4f}% versus SPY {100*f['spy_max_drawdown']:.4f}%. Average exposure {100*r['exposure']['mean']:.3f}%, peak {100*r['exposure']['max']:.3f}%. SPY holds overnight; this is not a matched-risk comparison.",
        f"- Classifier threshold-event ROC AUC {p['auc']:.6f}, five-session-block 95% interval [{auc['lower95']:.6f}, {auc['upper95']:.6f}], n={p['labeled']:,} across 59 sessions. Volatility alone event AUC {auc['volatility_score_auc']:.6f}; probability/volatility Spearman {p['probability_volatility_spearman']:.6f}.",
        f"- Directional-return AUC {move['positive_direction']['classifier_auc']:.6f}; absolute-move AUC {move['absolute_move']['classifier_auc']:.6f}. Probability also ranks negative-threshold events at {move['negative_threshold']['classifier_auc']:.6f}. This supports movement-magnitude-associated discrimination, not demonstrated directional skill or a causal claim about the network's internal mechanism.",
        f"- Canonical volatility: Pearson correlation {p['volatility_pearson']:.6f}, Spearman {p['volatility_spearman']:.6f}; MAE {p['volatility_mae']:.9f}, versus {p['volatility_metadata_mean_baseline_mae']:.9f} for the endpoint constant ({100*p['volatility_mae_reduction_vs_metadata_mean']:.3f}% lower). The baseline's estimation dataset is not certified as training-only. No second division by 1,000.",
        f"- Holding behavior: {t['extended_trades']}/{t['trades']} trades extended, median {t['holding_minutes']['median']:.0f} minutes. Extension-group results below are observational because extensions depend on later signals.",
        "- Numerical convention: strict cash-P&L signs give 286 wins, 258 losses and one exactly flat trade. A second trade has identical entry/exit prices but -2.842e-14 dollars cash-rounding residue; economically there are two flat-price trades. This is not a new losing market outcome. Win rate and material P&L are unaffected.",
        base.md_table(m['tables']['trades_by_extension'],['group','trades','win_rate','total_pnl','profit_factor','mean_return']),
        f"- Concentration: largest contributor {r['largest_ticker']} (${r['largest_ticker_pnl']:.2f}); other tickers together ${r['pnl_excluding_largest_ticker']:.2f}. Top five winners contribute {100*r['top5_winners_share_gross_wins']:.2f}% of gross wins and {100*r['top5_winners_share_net_pnl']:.2f}% of net P&L. Subtracting their contributions leaves ${r['pnl_excluding_top5_winners']:.2f}; this is accounting, not a strategy without those trades.",
        f"- Runtime: prediction round trips consume {e['prediction_seconds']:.3f}s; evaluation accounts for {e['stage_percent_of_cycle_work']['evaluation_ms']:.2f}% of cycle work. All batches contain 15 tickers, so there is no measured batch-size scaling relationship. Workload groups and latency correlations are retained in the full report/metrics.",
        f"- Auditable contract repair: {comp['capital_allocation_differences']['changed_requests']}/{comp['capital_allocation_differences']['entry_requests']} entry requests changed across actual trajectories, while signal eligibility and event timing remained invariant. See [the engineering comparison and case study](comparison.md). Financial differences do not validate the strategy.",
        "\n## C. Unsuitable for promotion\n",
        f"- Established alpha, expected live profitability, or superior risk management. Costs/spread/slippage are zero; only 59 sessions and a fixed universe are observed. Mean daily return 95% block interval [{100*f['mean_daily_return_bootstrap']['lower95']:.5f}%, {100*f['mean_daily_return_bootstrap']['upper95']:.5f}%]; excess-return interval [{100*f['mean_daily_excess_return_bootstrap']['lower95']:.5f}%, {100*f['mean_daily_excess_return_bootstrap']['upper95']:.5f}%].",
        f"- Robust cost tolerance: fixed-fill break-even is {r['linear_break_even_cost_bps']:.4f} bps per side. Annualized Sharpe {f['daily_sharpe_annualized_descriptive']:.3f} is a short-sample descriptive statistic, not a dependable headline.",
        base.md_table(r['cost_sensitivity_fixed_fills'],['one_way_bps','hypothetical_pnl']),
        "- Directional skill, independently validated profitable probability/volatility subgroups, tiny-ticker win rates, or causal benefits of extensions. Overlapping labels, shared shocks and exploratory subgroup search preclude those interpretations. No multiple-comparison-adjusted subgroup claims are made.",
        "- Canonical units alone do not establish calibrated portfolio-risk sizing or protective causality. This repair restored the serving contract; the allocation formula was neither redesigned nor optimized.",
        "- Production reliability/SLOs, sustained throughput, conversion-induced speedup or slowdown, or single-request latency obtained by dividing batch latency by 15. These are two local serial runs with already-running model services.",
        "\n## Five strongest overall facts\n",
        f"1. The corrected Java/Python CPU application processed 41,595 predictions in {e['full_backtest_wall_seconds']:.6f}s ({e['predictions_per_full_wall_second']:.3f}/s), covering 4,571 cycles and 545 completed trades.",
        f"2. Across 2,773 actual 15-ticker requests, median prediction round trip was {e['prediction_roundtrip_batch_ms']['median']:.4f}ms and p95 {e['prediction_roundtrip_batch_ms']['p95']:.4f}ms; intentional morning skips are excluded.",
        "3. Stable trade IDs and immutable artifacts reconcile 545 round trips, 4,550 extensions and 8,166 current position marks, with no failed executions and a flat terminal portfolio.",
        "4. Saved artifacts made an end-to-end unit-contract defect traceable and its regression-tested correction verifiable on the same predetermined period, preserving signal-path invariants.",
        f"5. Model diagnostics show descriptive structure: canonical-volatility realized correlation {p['volatility_pearson']:.3f} and event AUC {p['auc']:.3f}; directional AUC remains {move['positive_direction']['classifier_auc']:.3f}. This is useful model evaluation, not a demonstrated live trading edge.",
        "\nAll results retain 21 zero-volume vendor candles (420 input windows, 372 cycles), zero modeled transaction costs, overlapping horizons, a fixed ticker universe and a 59-session sample. Subgroups are descriptive, not independent strategies. No final resume bullets are provided.\n"]
    (out/"findings.md").write_text("\n\n".join(lines),encoding="utf-8")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output",type=Path,default=ROOT/"reports/showcase"/RUN_ID)
    args=parser.parse_args();out=validate_output(args.output)
    # Derived files may only go in a corrected-report directory, never into either run or legacy reports.
    reports=(ROOT/"reports/showcase").resolve();legacy_reports=reports/LEGACY_ID
    paths=[ROOT/"data/logs/runs"/RUN_ID,ROOT/"data/logs/runs"/LEGACY_ID,legacy_reports]
    before={str(p.relative_to(ROOT)):hashes(p) for p in paths}
    new=base.extract(paths[0]);old=base.extract(paths[1])
    assert new[0]["runId"]==RUN_ID and base.volatility_divisor(new[0])==1
    for name in ["python-model/training/cnn_threshold_classification/build_examples.py",
                 "python-model/training/cnn_volatility_regression/build_examples.py",
                 "python-model/training/cnn_volatility_regression/train.py"]:
        assert all(base.sha(ROOT/name)==run[0]["git"]["sourceFileSha256"][name] for run in [old,new]),name
    cm,daily=base.calculate(*new);lm,_=base.calculate(*old)
    model_diagnostics(cm,new[5])
    comp,allocations,equity=compare(old,new,lm,cm)
    cm["schema"]="showcase-analytics-v2"
    cm["provenance"]=dict(input_sha256=before[str(paths[0].relative_to(ROOT))],
        analysis_source_sha256={p.name:base.sha(p) for p in [Path(__file__),Path(base.__file__)]},
        python=platform.python_version(),numpy=np.__version__,pandas=pd.__version__,scipy=base.scipy.__version__,matplotlib=base.matplotlib.__version__,
        historical_session=new[0]["session"],models=new[0]["predictionService"]["models"],
        volatility_output_contract=new[0]["predictionService"]["volatilityOutputContract"],applied_volatility_divisor=1.0)
    out.mkdir(parents=True,exist_ok=True)
    for name,frame in [("cycles",new[4]),("predictions",new[5]),("trades",new[6]),("sessions",daily.reset_index()),
                       ("comparison_allocations",allocations),("comparison_equity",equity)]:jsonl(out/(name+".jsonl"),frame)
    base.write_json(out/"metrics.json",cm);base.write_json(out/"comparison.json",comp)
    base.plots(out,new[4],new[5],new[6],daily);base.write_report(out,cm)
    with (out/"report.md").open("a",encoding="utf-8") as stream:
        stream.write("\n## Corrected units, model diagnostics and engineering comparison\n\nCanonical API volatility is used unchanged (divisor 1). The legacy run is solely an engineering comparison. See [comparison.md](comparison.md) for matched trajectories, financial/latency differences and the defect case study; [findings.md](findings.md) for A/B/C classification.\n\n")
        stream.write(base.md_table([dict(outcome=k,**v) for k,v in cm["prediction"]["movement_diagnostics"].items() if isinstance(v,dict)], ["outcome","n","positive_count","classifier_auc","volatility_score_auc"]))
        direction=cm["prediction"]["movement_diagnostics"]["positive_direction"]["session_block_uncertainty"]
        stream.write(f"\n\nDirectional AUC five-session-block 95% interval: [{direction['lower95']:.6f}, {direction['upper95']:.6f}], containing 0.5. No demonstrated directional skill.\n")
        stream.write("\nStrict cash-P&L sign counts include one unchanged-price trade with -2.842e-14 dollars of floating-point residue. Both runs have two economically flat-price trades; see comparison.md.\n")
        stream.write("\n\nRegenerate with `python analytics/analyze_corrected.py`; see [METHODS_CORRECTED.md](../../../analytics/METHODS_CORRECTED.md).\n")
    write_findings(out,cm,comp);write_comparison(out,comp)
    after={str(p.relative_to(ROOT)):hashes(p) for p in paths}
    assert before==after,"Immutable inputs or legacy analytics changed"
    base.write_json(out/"analysis_completion.json",dict(run_id=RUN_ID,input_unchanged=True,legacy_reports_unchanged=True,
        inputs=after,analysis_source_sha256=cm["provenance"]["analysis_source_sha256"],
        output_sha256={p.name:base.sha(p) for p in out.iterdir() if p.is_file() and p.name!="analysis_completion.json"}))
    print(json.dumps(base.clean(dict(output=out,financial=cm["financial"],trade_summary=cm["trades"],
        engineering={k:cm["engineering"][k] for k in ["full_backtest_wall_seconds","predictions_per_full_wall_second","prediction_roundtrip_batch_ms"]},
        differences=comp["capital_allocation_differences"],invariants=comp["signal_path_invariants"])),indent=2))


if __name__=="__main__":main()
