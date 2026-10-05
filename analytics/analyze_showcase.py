"""Offline, deterministic analysis of one immutable showcase run; never calls the strategy."""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from contextlib import closing
import hashlib
import json
import math
from pathlib import Path
import platform
import sqlite3

import numpy as np
import pandas as pd
import scipy
from scipy.stats import rankdata, spearmanr
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

RUN_ID = "4b966e0f-d9c1-4de3-8a09-5d4b5af44332"
CORRECTED_RUN_ID = "2ef02c28-189c-447a-af1e-880d15f9e575"
ROOT = Path(__file__).resolve().parents[1]
SEED = 20260930
PROB_EDGES = [0, .1, .2, .25, .3, .4, .5, .75, 1.00000001]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def clean(value):
    if isinstance(value, dict):
        return {str(k): clean(v) for k, v in value.items()}
    if isinstance(value, (list, tuple, np.ndarray)):
        return [clean(v) for v in value]
    if isinstance(value, (np.integer, np.bool_)):
        return value.item()
    if isinstance(value, (float, np.floating)):
        return float(value) if math.isfinite(value) else None
    if isinstance(value, (pd.Timestamp, Path)):
        return str(value)
    return value


def write_json(path, value):
    path.write_text(json.dumps(clean(value), indent=2, allow_nan=False) + "\n", encoding="utf-8")


def distribution(values):
    a = np.asarray(values, dtype=float)
    a = a[np.isfinite(a)]
    if not len(a):
        return {"n": 0}
    return dict(n=len(a), mean=a.mean(), min=a.min(), p25=np.quantile(a, .25),
                median=np.median(a), p75=np.quantile(a, .75), p90=np.quantile(a, .9),
                p95=np.quantile(a, .95), p99=np.quantile(a, .99), max=a.max())


def auc(y, p):
    y, p = np.asarray(y), np.asarray(p)
    positives, negatives = (y == 1).sum(), (y == 0).sum()
    if not positives or not negatives:
        return np.nan
    return (rankdata(p)[y == 1].sum() - positives * (positives + 1) / 2) / (positives * negatives)


def correlation(a, b, rank=False):
    a, b = np.asarray(a, dtype=float), np.asarray(b, dtype=float)
    valid = np.isfinite(a) & np.isfinite(b)
    a, b = a[valid], b[valid]
    if len(a) < 3 or np.std(a) == 0 or np.std(b) == 0:
        return np.nan
    return float(spearmanr(a, b).statistic if rank else np.corrcoef(a, b)[0, 1])


def block_interval(numerators, denominators=None, block=5, draws=2000):
    """Circular moving-session-block bootstrap of a mean or pooled ratio; no IID trade assumption."""
    a = np.asarray(numerators, dtype=float)
    b = np.ones(len(a)) if denominators is None else np.asarray(denominators, dtype=float)
    rng = np.random.default_rng(SEED)
    starts = rng.integers(0, len(a), size=(draws, math.ceil(len(a) / block)))
    indices = ((starts[..., None] + np.arange(block)) % len(a)).reshape(draws, -1)[:, :len(a)]
    den = b[indices].sum(axis=1)
    samples = np.divide(a[indices].sum(axis=1), den, out=np.full(draws, np.nan), where=den > 0)
    return dict(estimate=a.sum() / b.sum(), lower95=np.nanquantile(samples, .025),
                upper95=np.nanquantile(samples, .975), sessions=len(a), block_sessions=block,
                resamples=draws, seed=SEED)


def auc_block_interval(y, scores, days, reference_scores, block=5, draws=2000):
    """Weighted tied-rank AUC under session-block resampling, including paired score comparison."""
    y = np.asarray(y)
    codes, names = pd.factorize(days, sort=True)
    def prepare(values):
        order = np.argsort(values, kind="stable")
        values = np.asarray(values)[order]
        return order, np.r_[0, np.flatnonzero(np.diff(values)) + 1]
    prepared = [prepare(scores), prepare(reference_scores)]
    rng = np.random.default_rng(SEED)
    samples = []
    for _ in range(draws):
        starts = rng.integers(0,len(names),size=math.ceil(len(names)/block))
        indices = ((starts[:,None]+np.arange(block))%len(names)).ravel()[:len(names)]
        weights = np.bincount(indices,minlength=len(names))[codes]
        pair = []
        for order, first in prepared:
            pos = np.add.reduceat((weights*y)[order],first)
            neg = np.add.reduceat((weights*(1-y))[order],first)
            pair.append(np.sum(pos*(np.cumsum(neg)-.5*neg))/(pos.sum()*neg.sum()))
        samples.append(pair)
    samples = np.asarray(samples)
    return dict(estimate=auc(y,scores),lower95=np.quantile(samples[:,0],.025),upper95=np.quantile(samples[:,0],.975),
        volatility_score_auc=auc(y,reference_scores),
        paired_auc_advantage_over_volatility=auc(y,scores)-auc(y,reference_scores),
        paired_advantage_lower95=np.quantile(samples[:,0]-samples[:,1],.025),
        paired_advantage_upper95=np.quantile(samples[:,0]-samples[:,1],.975),
        sessions=len(names),block_sessions=block,resamples=draws,seed=SEED)


def target_outcome(data, ticker, time, horizon=6):
    """Last completed input opens at t-5m; six future closes end at t+30m."""
    stamps = [time + (i - 1) * 300 for i in range(horizon + 1)]
    rows = [data.get((ticker, stamp)) for stamp in stamps]
    if any(row is None for row in rows):
        return None
    closes = np.asarray([row[3] for row in rows])
    returns = closes[1:] / closes[:-1] - 1
    return dict(target_return=closes[-1] / closes[0] - 1,
                realized_volatility=float(np.std(returns, ddof=0)),
                target_end_open=stamps[-1], target_realized_at=stamps[-1] + 300)


def drawdown(equity, initial):
    a = np.asarray(equity, dtype=float)
    peaks = np.maximum.accumulate(np.r_[initial, a])[1:]
    return a / peaks - 1


def classification_stats(frame):
    f = frame.dropna(subset=["target_return"])
    result = dict(predictions=len(frame), labeled=len(f), unlabeled=len(frame) - len(f),
                  labeled_sessions=f.day.nunique() if "day" in f else None)
    if not len(f):
        return result
    y, p = f.label.to_numpy(), f.probability.to_numpy()
    clipped = np.clip(p, 1e-12, 1 - 1e-12)
    result.update(positive_rate=y.mean(), mean_probability=p.mean(),
                  mean_fixed_target_return=f.target_return.mean(),
                  median_fixed_target_return=f.target_return.median(),
                  mean_open_to_open_return=f.open_return_30m.mean(),
                  open_return_labeled=int(f.open_return_30m.notna().sum()),
                  brier=np.mean((p - y) ** 2), auc=auc(y, p),
                  log_loss=-np.mean(y * np.log(clipped) + (1-y) * np.log(1-clipped)),
                  mean_predicted_volatility_return_units=f.volatility_return_units.mean(),
                  mean_realized_volatility=f.realized_volatility.mean(),
                  volatility_mae=np.mean(np.abs(f.volatility_return_units - f.realized_volatility)))
    return result


def trade_stats(f):
    if not len(f):
        return {"trades": 0}
    wins, losses = f.loc[f.pnl > 0, "pnl"], f.loc[f.pnl < 0, "pnl"]
    return dict(trades=len(f), sessions=f.entry_day.nunique(), wins=len(wins), losses=len(losses), flat=int((f.pnl == 0).sum()),
                win_rate=(f.pnl > 0).mean(), total_pnl=f.pnl.sum(), mean_pnl=f.pnl.mean(),
                median_pnl=f.pnl.median(), mean_return=f.trade_return.mean(), median_return=f.trade_return.median(),
                entry_capital_weighted_return=f.pnl.sum() / f.entry_notional.sum(),
                gross_wins=wins.sum(), gross_losses=losses.sum(),
                profit_factor=wins.sum() / -losses.sum() if len(losses) else np.nan,
                mean_win=wins.mean(), mean_loss=losses.mean(),
                holding_minutes=distribution(f.holding_minutes), extensions=distribution(f.extensions),
                extended_trades=int((f.extensions > 0).sum()),
                mean_mfe=f.mfe.mean(), mean_mae=f.mae.mean())


def grouped(frame, columns, fn):
    return [dict(group=list(key) if isinstance(key, tuple) else [str(key)], **fn(f))
            for key, f in frame.groupby(columns, observed=True, sort=True)]


def volatility_divisor(manifest):
    """Only the identified legacy run lacks explicit API unit provenance."""
    service = manifest["predictionService"]
    contract = service.get("volatilityOutputContract")
    if contract is not None:
        if (contract.get("version") == "volatility-return-fraction-v1"
                and contract.get("units") == "return_fraction"
                and contract.get("inverseTargetScalingApplied") is True):
            return 1.0
        raise ValueError("Unsupported volatility output contract")
    if manifest.get("runId") != RUN_ID:
        raise ValueError("Missing volatility output contract for an unidentified legacy run")
    model = next(x for x in service["models"] if x.get("modelId") == "cnn-volatility-v1")
    if model.get("sha256") != "c7a14302c33c59ea8b0daa9169cfc2f66c313b9568d7a792c77ed048cfd9112f":
        raise ValueError("Legacy volatility model identity does not match")
    scale = model.get("target_scale")
    if (isinstance(scale, bool) or not isinstance(scale, (int, float))
            or not math.isfinite(scale) or scale <= 0):
        raise ValueError("Legacy target_scale must be finite and positive")
    return float(scale)


def extract(run):
    manifest = json.loads((run / "manifest.json").read_text())
    completion = json.loads((run / "completion.json").read_text())
    session = json.loads((run / "session.json").read_text())
    assert completion["status"] == "COMPLETED"
    assert sha(run / "session.json") == completion["sessionSha256"]
    assert sha(run / "ohlcv.db") == manifest["data"]["sha256"]
    assert manifest["runId"] in (RUN_ID, CORRECTED_RUN_ID)
    assert all(x["runId"] == manifest["runId"] for x in (completion, session))
    with closing(sqlite3.connect((run / "ohlcv.db").as_uri() + "?mode=ro&immutable=1", uri=True)) as con:
        con.execute("PRAGMA query_only=ON")
        assert con.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
        raw = con.execute("SELECT ticker,timestamp,open,high,low,close,volume FROM ohlcv WHERE interval='FIVE_MINUTES' AND timestamp>946684800").fetchall()
    data = {(row[0], row[1]): row[2:] for row in raw}
    tickers = manifest["session"]["tickers"]
    scale = volatility_divisor(manifest)
    threshold = next(x["return_30m_threshold"] for x in manifest["predictionService"]["models"] if "return_30m_threshold" in x)
    signal_cutoff = manifest["strategy"]["parameters"]["minConfidenceThreshold"]
    cycles, signals, trades = [], [], {}
    equity0 = session["initialPortfolio"]["totalEquity"]
    for cycle in session["cycleLogs"]:
        time = pd.Timestamp(cycle["cycleTime"])
        stamp = int(time.timestamp())
        day = str(time.tz_convert("America/New_York").date())
        ev, val = cycle["evaluation"], cycle["portfolioAfterCycle"]
        assert all((ticker,stamp) in data for ticker in tickers)
        usable=[ticker for ticker in tickers if all((ticker,stamp-i*300) in data for i in range(1,32))]
        assert ev["usableTickers"]==usable
        if usable:
            assert len(usable)==15 and ev["inferenceStatus"]=="COMPLETED" and ev["predictionDurationNanos"]>0
            assert len(ev["signals"])==15 and {s["ticker"] for s in ev["signals"]}==set(tickers)
        else:
            local=time.tz_convert("America/New_York")
            assert local.hour*60+local.minute < 12*60+5
            assert ev["inferenceStatus"]=="SKIPPED_EMPTY_WINDOWS" and ev["inferenceBatchSize"]==0 and not ev["signals"]
        assert val["status"] == "COMPLETE" and val["samplingPoint"] == "POST_CYCLE"
        assert cycle["cycleFailure"] is None
        values = [p["value"] for p in val["positions"].values()]
        invested = sum(values)
        assert np.isclose(val["totalEquity"], val["cash"] + invested, rtol=1e-12)
        for ticker, pos in val["positions"].items():
            assert pos["status"] == "CURRENT" and pos["priceTimestamp"] == cycle["cycleTime"]
            assert np.isclose(pos["price"], data[ticker, stamp][0])
        row = dict(time=str(time), stamp=stamp, day=day, equity=val["totalEquity"], cash=val["cash"],
                   exposure=invested / val["totalEquity"], positions=len(values),
                   max_ticker_weight=max(values, default=0) / val["totalEquity"],
                   top3_equity_weight=sum(sorted(values, reverse=True)[:3]) / val["totalEquity"],
                   invested_hhi=sum((v / invested)**2 for v in values) if invested else 0,
                   inference=ev["inferenceStatus"] == "COMPLETED", batch_size=ev["inferenceBatchSize"],
                   entries=len(cycle["successfulEntryExecutions"]), exits=len(cycle["successfulExitExecutions"]),
                   adjustments=len(cycle["appliedOpenTradeAdjustments"]))
        row.update({k.removesuffix("Nanos") + "_ms": v / 1e6 for k, v in cycle["timing"].items()})
        row.update({k.replace("DurationNanos", "_ms"): ev[k] / 1e6 for k in ["windowDurationNanos", "predictionDurationNanos", "decisionDurationNanos"]})
        row.update({"weight_" + ticker: val["positions"].get(ticker, {}).get("value", 0) / val["totalEquity"] for ticker in tickers})
        cycles.append(row)
        signal_map = {s["ticker"]: s for s in ev["signals"]}
        for sig in ev["signals"]:
            ticker = sig["ticker"]
            out = dict(sig, time=str(time), stamp=stamp, day=day,
                       volatility_return_units=sig["volatility"] / scale,
                       passes_probability=sig["probability"] >= signal_cutoff,
                       allocated=sig["reason"] in ["CAPPED_ALLOCATION", "ALLOCATION_REQUESTED"],
                       extended=sig["reason"] == "EXTEND_EXISTING_TRADE",
                       zero_volume_input=any(data.get((ticker, stamp - i*300), (0,0,0,0,1))[4] == 0 for i in range(1,32)))
            outcome = target_outcome(data, ticker, stamp)
            out.update(outcome or dict(target_return=np.nan, realized_volatility=np.nan,
                                      target_end_open=None, target_realized_at=None))
            out["label"] = float(out["target_return"] >= threshold) if outcome else np.nan
            future = data.get((ticker, stamp + 1800))
            out["open_return_30m"] = future[0] / data[ticker, stamp][0] - 1 if future else np.nan
            signals.append(out)
        for fill in cycle["successfulEntryExecutions"]:
            sig = signal_map[fill["ticker"]]
            assert fill["tradeId"] not in trades
            trades[fill["tradeId"]] = dict(trade_id=fill["tradeId"], ticker=fill["ticker"],
                entry_time=fill["executionTime"], entry_stamp=stamp, entry_day=day,
                entry_price=fill["price"], quantity=fill["quantity"], entry_notional=fill["cashAmount"],
                probability=sig["probability"], volatility=sig["volatility"],
                volatility_return_units=sig["volatility"] / scale, extensions=0)
        for adj in cycle["appliedOpenTradeAdjustments"]:
            trades[adj["targetTrade"]["tradeId"]]["extensions"] += 1
        for fill in cycle["successfulExitExecutions"]:
            finish_trade(trades, fill, day, data)
        assert not cycle["failedEntryExecutions"] and not cycle["failedExitExecutions"]
    for fill in session["liquidationResult"]["successfulExecutions"]:
        finish_trade(trades, fill, str(pd.Timestamp(fill["executionTime"]).tz_convert("America/New_York").date()), data)
    assert session["liquidationResult"]["registryEmptyAfter"] and not session["remainingOpenTrades"]
    c, p, t = pd.DataFrame(cycles), pd.DataFrame(signals), pd.DataFrame(trades.values())
    calendar=manifest["marketCalendar"]
    expected=[]
    for time in pd.date_range(c.time.iloc[0],c.time.iloc[-1],freq="5min"):
        local=time.tz_convert(calendar["timezone"]);date=str(local.date())
        close=calendar["earlyCloseDates"].get(date,calendar["regularClose"])
        close_minutes=int(close[:2])*60+int(close[3:])
        open_minutes=int(calendar["regularOpen"][:2])*60+int(calendar["regularOpen"][3:])
        if local.weekday()<5 and date not in calendar["fullCloseDates"] and open_minutes<=local.hour*60+local.minute<=close_minutes-5:
            expected.append(int(time.timestamp()))
    assert c.stamp.tolist()==expected
    assert (len(c), int(c.inference.sum()), len(p), len(t)) == (4571, 2773, 41595, 545)
    assert (c.loc[c.inference, "batch_size"] == 15).all()
    assert t.exit_price.notna().all() and t.trade_id.is_unique
    assert np.isclose(t.pnl.sum(), session["finalPortfolio"]["totalEquity"] - equity0)
    assert np.isclose(c.equity.iloc[-1], session["finalPortfolio"]["totalEquity"])
    return manifest, completion, session, data, c, p, t


def finish_trade(trades, fill, day, data):
    t = trades[fill["tradeId"]]
    assert "exit_price" not in t and np.isclose(fill["quantity"], t["quantity"])
    exit_stamp = int(pd.Timestamp(fill["executionTime"]).timestamp())
    rows = [data[t["ticker"], stamp] for stamp in range(t["entry_stamp"], exit_stamp, 300)]
    t.update(exit_time=fill["executionTime"], exit_day=day, exit_price=fill["price"], exit_notional=fill["cashAmount"],
             pnl=fill["cashAmount"] - t["entry_notional"], trade_return=fill["price"] / t["entry_price"] - 1,
             holding_minutes=(exit_stamp - t["entry_stamp"]) / 60,
             mfe=max(0, max(r[1] for r in rows)/t["entry_price"] - 1, fill["price"]/t["entry_price"]-1),
             mae=min(0, min(r[2] for r in rows)/t["entry_price"] - 1, fill["price"]/t["entry_price"]-1))


def verify_integrity(session, data):
    cash=session["startingCash"];positions={};registry={};seen=set();closed=set();marks=0;extensions=0
    def apply(fill,time):
        nonlocal cash
        ticker,q,price=fill["ticker"],fill["quantity"],fill["price"]
        assert fill["executionTime"]==time and np.isclose(price,data[ticker,int(pd.Timestamp(time).timestamp())][0])
        assert np.isclose(fill["cashAmount"],q*price)
        if fill["action"]=="BUY":
            cash-=q*price;positions[ticker]=positions.get(ticker,0)+q
        else:
            cash+=q*price;positions[ticker]-=q
            if positions[ticker]==0:del positions[ticker]
        assert cash>=-1e-8 and all(q>=0 for q in positions.values())
    def compare(snapshot,valued=False):
        assert math.isclose(cash,snapshot["cash"],rel_tol=1e-10,abs_tol=1e-8)
        q={t:v["quantity"] if valued else v for t,v in snapshot["positions"].items()}
        assert set(q)==set(positions) and all(np.isclose(q[t],positions[t]) for t in q)
    for cycle in session["cycleLogs"]:
        time=cycle["cycleTime"]
        due={id:t for id,t in registry.items() if t["exitTime"]<=time}
        assert due=={t["tradeId"]:t for t in cycle["tradesDueForExit"]}
        assert due=={t["tradeId"]:t for t in cycle["successfullyClosedTrades"]}
        assert set(due)=={fill["tradeId"] for fill in cycle["successfulExitExecutions"]}
        for fill in cycle["successfulExitExecutions"]:
            id=fill["tradeId"]
            assert id in registry and id not in closed and np.isclose(fill["quantity"],registry[id]["quantity"])
            apply(fill,time);closed.add(id);del registry[id]
        compare(cycle["evaluation"]["portfolioSnapshot"])
        opened={t["tradeId"]:t for t in cycle["successfullyOpenedTrades"]}
        assert len(opened)==len(cycle["successfulEntryExecutions"])==len(cycle["attemptedCapitalAllocations"])
        for fill,allocation in zip(cycle["successfulEntryExecutions"],cycle["attemptedCapitalAllocations"]):
            id=fill["tradeId"];trade=opened[id]
            assert id not in seen and fill["ticker"]==trade["ticker"]==allocation["ticker"]
            assert np.isclose(fill["cashAmount"],allocation["cashAmount"]) and np.isclose(fill["quantity"],trade["quantity"])
            apply(fill,time);seen.add(id);registry[id]=dict(trade)
        assert cycle["evaluation"]["openTradeAdjustments"]==cycle["appliedOpenTradeAdjustments"]
        for adj in cycle["appliedOpenTradeAdjustments"]:
            target=adj["targetTrade"];id=target["tradeId"]
            assert registry[id]==target and adj["newExitTime"]>target["exitTime"]
            registry[id]=dict(target,exitTime=adj["newExitTime"]);extensions+=1
        compare(cycle["portfolioAfterCycle"],True)
        totals=defaultdict(float)
        for trade in registry.values():totals[trade["ticker"]]+=trade["quantity"]
        assert set(totals)==set(positions) and all(np.isclose(totals[t],positions[t]) for t in totals)
        marks+=len(cycle["portfolioAfterCycle"]["positions"])
    liquidation=session["liquidationResult"]
    assert {t["tradeId"]:t for t in liquidation["attemptedTrades"]}==registry
    for fill in liquidation["successfulExecutions"]:
        id=fill["tradeId"];assert id in registry and id not in closed
        apply(fill,liquidation["liquidationTime"]);closed.add(id);del registry[id]
    assert not liquidation["failedExecutions"] and not registry and seen==closed
    compare(session["finalPortfolio"],True)
    assert not positions and session["finalPortfolio"]["status"]=="COMPLETE"
    return dict(reconciled_round_trips=len(seen),stable_id_extensions=extensions,current_marks=marks,
                failed_executions=0,all_cash_position_registry_checks_passed=True)


def calculate(manifest, completion, session, data, c, p, t):
    initial = session["startingCash"]
    start, end = int(c.stamp.iloc[0]), int(c.stamp.iloc[-1])
    spy_start, spy_end = data["SPY", start][0], data["SPY", end][0]
    c["spy_equity"] = [initial * data["SPY", stamp][0] / spy_start for stamp in c.stamp]
    c["drawdown"] = drawdown(c.equity, initial)
    c["spy_drawdown"] = drawdown(c.spy_equity, initial)
    daily = c.groupby("day", sort=True).agg(equity=("equity", "last"), spy_equity=("spy_equity", "last"),
                mean_exposure=("exposure", "mean"), worst_drawdown=("drawdown", "min"))
    daily["strategy_return"] = daily.equity / daily.equity.shift(1, fill_value=initial) - 1
    daily["spy_return"] = daily.spy_equity / daily.spy_equity.shift(1, fill_value=initial) - 1
    daily["excess_return"] = daily.strategy_return - daily.spy_return
    daily["trade_pnl"] = t.groupby("exit_day").pnl.sum().reindex(daily.index, fill_value=0)
    daily["trade_count"] = t.groupby("exit_day").size().reindex(daily.index, fill_value=0)
    p["probability_bucket"] = pd.cut(p.probability, PROB_EDGES, right=False).astype(str)
    p["volatility_bucket"], vol_edges = pd.qcut(p.volatility_return_units, 4, labels=False, retbins=True, duplicates="drop")
    t["probability_bucket"] = pd.cut(t.probability, PROB_EDGES, right=False).astype(str)
    t["volatility_bucket"] = pd.cut(t.volatility_return_units, np.r_[-np.inf, vol_edges[1:-1], np.inf], labels=False)
    t["extended"] = t.extensions > 0
    valid = p.dropna(subset=["target_return"]).copy()
    base_rate = valid.label.mean()
    valid["brier_advantage_vs_sample_base"] = (valid.label-base_rate)**2 - (valid.probability-valid.label)**2
    valid["vol_abs_error"] = (valid.volatility_return_units-valid.realized_volatility).abs()
    valid["vol_baseline_abs_error"] = (manifest["endpoint"]["outputStatistics"]["volatilityMean"]-valid.realized_volatility).abs()
    valid["vol_mae_advantage"] = valid.vol_baseline_abs_error-valid.vol_abs_error
    by_day = valid.groupby("day").agg(brier_sum=("brier_advantage_vs_sample_base", "sum"),
        vol_mae_sum=("vol_mae_advantage", "sum"), n=("label", "size"))
    trough = int(c.drawdown.argmin())
    path = np.r_[initial, c.equity.to_numpy()]
    peak_index = int(np.argmax(path[:trough+2]))
    peak_time = c.time.iloc[max(0, peak_index-1)]
    peak_equity = path[peak_index]
    recovery_rows = c.iloc[trough+1:][c.equity.iloc[trough+1:] >= peak_equity]
    trade_summary = trade_stats(t)
    trade_summary["longest_losing_streak_entry_order"] = longest_true_run(t.pnl < 0)
    trade_summary["longest_winning_streak_entry_order"] = longest_true_run(t.pnl > 0)
    top_positive = t.loc[t.pnl > 0, "pnl"].nlargest(5).sum()
    per_ticker = t.groupby("ticker").pnl.sum()
    financial = dict(initial_equity=initial, terminal_equity=c.equity.iloc[-1], gross_pnl=t.pnl.sum(),
        total_return=c.equity.iloc[-1]/initial-1, spy_total_return=spy_end/spy_start-1,
        excess_return_percentage_points=(c.equity.iloc[-1]/initial-spy_end/spy_start)*100,
        max_drawdown=c.drawdown.min(), spy_max_drawdown=c.spy_drawdown.min(),
        drawdown_peak_time=peak_time, drawdown_trough_time=c.time.iloc[trough],
        drawdown_recovery_time=recovery_rows.time.iloc[0] if len(recovery_rows) else None,
        max_drawdown_duration_market_samples=longest_true_run(c.drawdown < -1e-12),
        sessions=len(daily), daily_return=distribution(daily.strategy_return),
        positive_sessions=int((daily.strategy_return > 0).sum()),
        daily_sharpe_annualized_descriptive=daily.strategy_return.mean()/daily.strategy_return.std(ddof=1)*np.sqrt(252),
        daily_volatility_annualized_descriptive=daily.strategy_return.std(ddof=1)*np.sqrt(252),
        daily_correlation_to_spy=correlation(daily.strategy_return, daily.spy_return),
        daily_beta_to_spy=np.cov(daily.strategy_return, daily.spy_return, ddof=1)[0,1]/np.var(daily.spy_return,ddof=1),
        mean_daily_return_bootstrap=block_interval(daily.strategy_return),
        mean_daily_excess_return_bootstrap=block_interval(daily.excess_return),
        benchmark=dict(ticker="SPY", start_time=c.time.iloc[0], end_time=c.time.iloc[-1],
                       start_open=spy_start, end_open=spy_end, fractional_shares=initial/spy_start,
                       methodology="Buy at first cycle candle open; hold overnight; sell at final cycle candle open. Same initial capital and zero modeled costs. Unadjusted price return, no dividends/corporate-action accounting."))
    prediction = classification_stats(p)
    prediction.update(probability=distribution(p.probability), volatility_logged=distribution(p.volatility),
        volatility_return_units=distribution(p.volatility_return_units), target_scale=next(x["target_scale"] for x in manifest["predictionService"]["models"] if "target_scale" in x),
        sample_base_rate_brier=base_rate*(1-base_rate), sample_base_rate_log_loss=-(base_rate*np.log(base_rate)+(1-base_rate)*np.log(1-base_rate)),
        brier_advantage_bootstrap=block_interval(by_day.brier_sum, by_day.n),
        volatility_mae_advantage_bootstrap=block_interval(by_day.vol_mae_sum, by_day.n),
        volatility_rmse=np.sqrt(np.mean((valid.volatility_return_units-valid.realized_volatility)**2)),
        volatility_bias=(valid.volatility_return_units-valid.realized_volatility).mean(),
        volatility_pearson=correlation(valid.volatility_return_units, valid.realized_volatility),
        volatility_spearman=correlation(valid.volatility_return_units, valid.realized_volatility, True),
        volatility_metadata_mean_baseline_mae=valid.vol_baseline_abs_error.mean(),
        mean_realized_volatility=valid.realized_volatility.mean(),
        fixed_target_vs_executable_return_correlation=correlation(valid.target_return, valid.open_return_30m),
        labels_realizing_after_last_execution=int((valid.target_realized_at > end).sum()))
    prediction["auc_session_block_uncertainty"] = auc_block_interval(valid.label.to_numpy(),valid.probability.to_numpy(),
        valid.day.to_numpy(),valid.volatility_return_units.to_numpy())
    prediction["probability_volatility_spearman"] = correlation(valid.probability,valid.volatility_return_units,True)
    prediction["majority_class_accuracy"] = max(base_rate,1-base_rate)
    prediction["brier_skill_vs_sample_base"] = 1-prediction["brier"] / prediction["sample_base_rate_brier"]
    prediction["volatility_mae_reduction_vs_metadata_mean"] = 1-prediction["volatility_mae"] / prediction["volatility_metadata_mean_baseline_mae"]
    for cutoff in [.25, .5]:
        yes = valid.probability >= cutoff
        tp, fp = int((yes & (valid.label == 1)).sum()), int((yes & (valid.label == 0)).sum())
        fn, tn = int((~yes & (valid.label == 1)).sum()), int((~yes & (valid.label == 0)).sum())
        prediction["threshold_" + str(cutoff)] = dict(tp=tp, fp=fp, fn=fn, tn=tn,
            precision=tp/(tp+fp) if tp+fp else None, recall=tp/(tp+fn), accuracy=(tp+tn)/len(valid))
    active = c[c.exposure > 0]
    risk = dict(exposure=distribution(c.exposure), invested_cycle_fraction=len(active)/len(c),
        inference_cycle_exposure=distribution(c.loc[c.inference,"exposure"]),
        concurrent_tickers=distribution(c.positions), max_single_ticker_equity_weight=c.max_ticker_weight.max(),
        max_top3_equity_weight=c.top3_equity_weight.max(), invested_hhi=distribution(active.invested_hhi),
        exposure_drawdown_correlation=correlation(c.exposure,c.drawdown),
        daily_exposure_daily_return_correlation=correlation(daily.mean_exposure,daily.strategy_return),
        exposure_by_ticker={ticker:distribution(c["weight_"+ticker]) for ticker in manifest["session"]["tickers"]},
        equity_turnover=(t.entry_notional.sum()+t.exit_notional.sum())/c.equity.mean(),
        total_traded_notional=t.entry_notional.sum()+t.exit_notional.sum(),
        linear_break_even_cost_bps=t.pnl.sum()/(t.entry_notional.sum()+t.exit_notional.sum())*10000,
        cost_sensitivity_fixed_fills=[dict(one_way_bps=bps, hypothetical_pnl=t.pnl.sum()-(t.entry_notional.sum()+t.exit_notional.sum())*bps/10000) for bps in [1,5,10]],
        top5_winners_share_gross_wins=top_positive/t.loc[t.pnl>0,"pnl"].sum(),
        top5_winners_share_net_pnl=top_positive/t.pnl.sum() if t.pnl.sum()!=0 else None,
        largest_ticker_pnl=per_ticker.max(), largest_ticker=str(per_ticker.idxmax()),
        pnl_excluding_top5_winners=t.pnl.sum()-top_positive,
        pnl_excluding_largest_ticker=t.pnl.sum()-per_ticker.max())
    inf = c[c.inference]
    full_seconds = completion["fullBacktestWallNanos"]/1e9
    stages = ["exit_ms", "evaluation_ms", "entryAndAdjustment_ms", "valuation_ms"]
    engineering = dict(market_cycles=len(c), inference_cycles=len(inf), warmup_cycles=len(c)-len(inf),
        ticker_predictions=len(p), completed_trades=len(t), batch_size=15,
        full_backtest_wall_seconds=full_seconds, driver_seconds=completion["driverNanos"]/1e9,
        session_write_seconds=completion["sessionWriteNanos"]/1e9,
        setup_and_completion_residual_seconds=(completion["fullBacktestWallNanos"]-completion["driverNanos"]-completion["sessionWriteNanos"])/1e9,
        predictions_per_full_wall_second=len(p)/full_seconds, cycles_per_full_wall_second=len(c)/full_seconds,
        predictions_per_prediction_roundtrip_second=len(p)/(inf.prediction_ms.sum()/1000),
        inference_cycle_total_ms=distribution(inf.total_ms), warmup_cycle_total_ms=distribution(c.loc[~c.inference,"total_ms"]),
        prediction_roundtrip_batch_ms=distribution(inf.prediction_ms),
        amortized_batch_ms_per_ticker=distribution(inf.prediction_ms/15),
        first_inference_ms=float(inf.prediction_ms.iloc[0]),
        later_inference_ms=distribution(inf.prediction_ms.iloc[1:]),
        stage_seconds={key:c[key].sum()/1000 for key in stages},
        stage_percent_of_cycle_work={key:100*c[key].sum()/c.total_ms.sum() for key in stages},
        window_seconds=c.window_ms.sum()/1000,
        prediction_seconds=c.prediction_ms.sum()/1000,
        decision_seconds=c.decision_ms.sum()/1000,
        cycle_overhead_seconds=(c.total_ms.sum()-c[stages].sum().sum())/1000,
        runtime_environment=manifest["runtime"], model_runtime=manifest["predictionService"]["runtime"],
        inference_latency_spearman_with_positions=correlation(inf.total_ms,inf.positions,True),
        inference_latency_spearman_with_adjustments=correlation(inf.total_ms,inf.adjustments,True),
        work_groups=grouped(inf.assign(has_entries=inf.entries>0,has_exits=inf.exits>0),["has_entries","has_exits"],
                            lambda f:dict(cycles=len(f),cycle_ms=distribution(f.total_ms),prediction_ms=distribution(f.prediction_ms))))
    c["exposure_bucket"] = pd.cut(c.exposure,[-.001,0,.25,.5,.75,1.001],include_lowest=True).astype(str)
    tables = dict(trades_by_ticker=grouped(t,"ticker",trade_stats),
        trades_by_extension=grouped(t,"extended",trade_stats), trades_by_probability=grouped(t,"probability_bucket",trade_stats),
        trades_by_volatility=grouped(t,"volatility_bucket",trade_stats),
        predictions_by_ticker=grouped(p,"ticker",classification_stats),
        predictions_by_probability=grouped(p,"probability_bucket",classification_stats),
        predictions_by_volatility=grouped(p,"volatility_bucket",classification_stats),
        probability_volatility_interaction=grouped(p,["probability_bucket","volatility_bucket"],classification_stats),
        predictions_by_decision=grouped(p,"reason",classification_stats),
        predictions_by_probability_acceptance=grouped(p,"passes_probability",classification_stats),
        predictions_by_allocation=grouped(p,"allocated",classification_stats),
        predictions_by_zero_volume=grouped(p,"zero_volume_input",classification_stats),
        exposure_vs_drawdown=grouped(c,"exposure_bucket",lambda f:dict(cycles=len(f),mean_drawdown=f.drawdown.mean(),worst_drawdown=f.drawdown.min())))
    zeros = [(ticker,stamp,row) for (ticker,stamp),row in data.items() if ticker in manifest["session"]["tickers"] and start-9300<=stamp<=end and row[4]==0]
    coverage = dict(sessions=len(daily), candles_per_ticker=dict(Counter(ticker for ticker,stamp in data if ticker in manifest["session"]["tickers"] and start-9300<=stamp<=end)), tickers=len(manifest["session"]["tickers"]),
        zero_volume_candles=len(zeros), predictions_with_zero_volume_input=int(p.zero_volume_input.sum()),
        zero_volume_affected_cycles=int(p.loc[p.zero_volume_input,"stamp"].nunique()),
        zero_volume_rows=[dict(ticker=ticker,timestamp=stamp,ohlcv=row) for ticker,stamp,row in zeros],
        session_failures=0, cycle_failures=0, failed_entries=0, failed_exits=0,
        missing_or_stale_marks=0, partial_inference_batches=0, unanticipated_skips=0,
        labeled_predictions=len(valid), unlabeled_predictions=len(p)-len(valid),
        unlabeled_reason="Final five inference cycles of each session lack six contiguous future five-minute candles; no overnight stitching.")
    metrics = dict(schema="showcase-analytics-v1", run_id=manifest["runId"], financial=financial, trades=trade_summary,
        integrity=verify_integrity(session,data),
        risk=risk, prediction=prediction, engineering=engineering, coverage=coverage, tables=tables,
        volatility_quartile_edges_return_units=vol_edges,
        trade_mean_pnl_bootstrap=block_interval(daily.trade_pnl,daily.trade_count),
        bootstrap_limitations="59 sessions; circular moving blocks of 5 sessions, 2000 resamples, fixed seed. Preserve cross-ticker/within-session dependence. Intervals are exploratory, not corrected for subgroup search or evidence of future generalization.")
    return metrics, daily


def longest_true_run(values):
    longest = current = 0
    for value in values:
        current = current + 1 if value else 0
        longest = max(longest, current)
    return longest


def md_table(rows, columns):
    def fmt(x):
        if isinstance(x,list): return " / ".join(map(str,x))
        if isinstance(x,(float,np.floating)): return f"{x:.6g}" if np.isfinite(x) else "N/A"
        return str(x)
    return "| " + " | ".join(columns) + " |\n|" + "|".join("---" for _ in columns) + "|\n" + "\n".join("| " + " | ".join(fmt(row.get(k,"")) for k in columns) + " |" for row in rows)


def plots(out,c,p,t,daily):
    plt.rcParams.update({"figure.dpi":130,"savefig.dpi":150,"axes.spines.top":False,"axes.spines.right":False,"font.size":9})
    fig,axes=plt.subplots(2,1,figsize=(10,7),sharex=True)
    x=pd.to_datetime(c.time,utc=True)
    initial=10000
    axes[0].plot(x,100*(c.equity/initial-1),label="Strategy, gross simulation")
    axes[0].plot(x,100*(c.spy_equity/initial-1),label="SPY, price-only buy and hold")
    axes[0].set(ylabel="Cumulative return (%)",title="Fixed showcase: identical execution bounds; zero modeled costs")
    axes[0].legend()
    axes[1].plot(x,100*c.drawdown,label="Strategy");axes[1].plot(x,100*c.spy_drawdown,label="SPY")
    axes[1].set(ylabel="Drawdown (%)",xlabel="2026 date (UTC); no closed-market observations")
    fig.tight_layout();fig.savefig(out/"equity_drawdown.png");plt.close(fig)
    fig,axes=plt.subplots(1,2,figsize=(11,4))
    attrib=t.groupby("ticker").pnl.sum().sort_values()
    axes[0].barh(attrib.index,attrib.values,color=["#b84b4b" if v<0 else "#287a82" for v in attrib]);axes[0].set(title="All tickers: gross trade P&L",xlabel="Simulated dollars")
    axes[1].scatter(100*daily.mean_exposure,100*daily.strategy_return,s=25,alpha=.7)
    axes[1].set(title="Exposure versus daily return (59 sessions)",xlabel="Mean post-cycle exposure (%)",ylabel="Gross session return (%)")
    fig.tight_layout();fig.savefig(out/"attribution_exposure.png");plt.close(fig)
    fig,axes=plt.subplots(1,2,figsize=(11,4))
    valid=p.dropna(subset=["target_return"])
    calibration=valid.groupby("probability_bucket",observed=True).agg(probability=("probability","mean"),frequency=("label","mean"),n=("label","size"))
    axes[0].plot([0,1],[0,1],"--",color="gray");axes[0].scatter(calibration.probability,calibration.frequency)
    for row in calibration.itertuples():axes[0].annotate(str(row.n),(row.probability,row.frequency),fontsize=7)
    axes[0].set(title="Classifier calibration; labels show sample count",xlabel="Mean predicted probability",ylabel="Observed target frequency",xlim=(0,1),ylim=(0,1))
    g=valid.groupby("volatility_bucket").agg(predicted=("volatility_return_units","mean"),actual=("realized_volatility","mean"))
    axes[1].plot(g.index,100*g.predicted,"o-",label="Predicted (return units)");axes[1].plot(g.index,100*g.actual,"o-",label="Realized")
    axes[1].set(title="Volatility: six-return population standard deviation",xlabel="Predicted-volatility quartile (0=lowest)",ylabel="Five-minute return standard deviation (%)");axes[1].legend()
    axes[1].set_xticks([0,1,2,3])
    fig.tight_layout();fig.savefig(out/"prediction_diagnostics.png");plt.close(fig)
    fig,axes=plt.subplots(1,2,figsize=(11,4))
    latency=c.loc[c.inference,"prediction_ms"].sort_values().to_numpy()
    axes[0].plot(latency,np.arange(1,len(latency)+1)/len(latency));axes[0].set_xscale("log")
    axes[0].set(title="Batch prediction latency: 2,773 calls, 15 tickers each",xlabel="Round-trip ms, logarithmic scale",ylabel="Empirical cumulative fraction")
    stages=["exit_ms","evaluation_ms","entryAndAdjustment_ms","valuation_ms"]
    totals=[c[k].sum()/1000 for k in stages]
    bars=axes[1].barh(["Exits","Evaluation (includes HTTP + models)","Entries / extensions","Valuation"],totals)
    axes[1].bar_label(bars,labels=[f"{v:.3f}s" for v in totals],padding=3)
    axes[1].set_xlim(0,max(totals)*1.17)
    axes[1].set(title="Non-overlapping cycle stages",xlabel="Total seconds across 4,571 cycles")
    fig.tight_layout();fig.savefig(out/"engineering.png");plt.close(fig)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run",type=Path,default=ROOT/"data/logs/runs"/RUN_ID)
    parser.add_argument("--output",type=Path,default=ROOT/"reports/showcase"/RUN_ID)
    args=parser.parse_args();run=args.run.resolve();out=args.output.resolve()
    if out==run or run in out.parents or out in run.parents:
        raise ValueError("Derived output must be separate from, and not an ancestor of, the immutable run")
    before={p.name:sha(p) for p in run.iterdir() if p.is_file()}
    manifest,completion,session,data,c,p,t=extract(run)
    if manifest["runId"] != RUN_ID:
        raise ValueError("Use analyze_corrected.py for the corrected run and its separate output directory")
    metrics,daily=calculate(manifest,completion,session,data,c,p,t)
    out.mkdir(parents=True,exist_ok=True)
    metrics["provenance"]=dict(input_sha256=before,analysis_script_sha256=sha(__file__),
        python=platform.python_version(),numpy=np.__version__,pandas=pd.__version__,scipy=scipy.__version__,matplotlib=matplotlib.__version__,
        historical_session=manifest["session"],models=manifest["predictionService"]["models"],
        source_paths=["python-model/training/cnn_threshold_classification/build_examples.py",
                      "python-model/training/cnn_volatility_regression/build_examples.py",
                      "python-model/training/cnn_volatility_regression/train.py","python-model/app/model_server.py"])
    # The target definitions used by this analysis must match the run's hashed source.
    for name in metrics["provenance"]["source_paths"]:
        assert sha(ROOT/name)==manifest["git"]["sourceFileSha256"][name], "Target/source definition drift: "+name
    for name,frame in [("cycles",c),("predictions",p),("trades",t),("sessions",daily.reset_index())]:
        with (out/(name+".jsonl")).open("w",encoding="utf-8") as f:
            for record in frame.to_dict("records"):
                f.write(json.dumps(clean(record),allow_nan=False)+"\n")
    write_json(out/"metrics.json",metrics)
    plots(out,c,p,t,daily)
    write_report(out,metrics)
    write_findings(out,metrics)
    after={p.name:sha(p) for p in run.iterdir() if p.is_file()}
    assert before==after,"Immutable run changed during analysis"
    write_json(out/"analysis_completion.json",dict(run_id=RUN_ID,input_unchanged=True,input_sha256=after,
        output_sha256={f.name:sha(f) for f in out.iterdir() if f.is_file() and f.name!="analysis_completion.json"}))
    print(json.dumps(clean(dict(output=str(out),input_unchanged=True,
        gross_return=metrics["financial"]["total_return"],spy_return=metrics["financial"]["spy_total_return"],
        classifier_auc=metrics["prediction"]["auc_session_block_uncertainty"],
        inference_batch_ms=metrics["engineering"]["prediction_roundtrip_batch_ms"])),indent=2))


def write_report(out,m):
    f,e,p,r=m["financial"],m["engineering"],m["prediction"],m["risk"]
    report=[f"# Fixed showcase exploratory analytics\n\nRun `{m['run_id']}`. Gross historical simulation; no strategy rerun or parameter changes.\n",
      f"The strongest presentation evidence is engineering and auditability. The run handled 41,595 ticker predictions in {e['full_backtest_wall_seconds']:.3f}s; full 15-ticker round trips had {e['prediction_roundtrip_batch_ms']['median']:.3f}ms median latency. Gross strategy return was {100*f['total_return']:.3f}% versus SPY {100*f['spy_total_return']:.3f}%, with limited cost headroom and no established trading alpha. The 21 zero-volume candles remain disclosed. Volatility units are decoded from provenance; this report does not change any recorded fills. See [findings.md](findings.md) for complete A/B/C classifications and qualifications.\n",
      f"Benchmark: buy {f['benchmark']['fractional_shares']:.9f} fractional SPY shares at ${f['benchmark']['start_open']:.9f} on {f['benchmark']['start_time']}; sell at ${f['benchmark']['end_open']:.9f} on {f['benchmark']['end_time']}. Same $10,000 initial capital, exact candle-open execution, zero modeled costs and no dividends/interest. SPY holds overnight; the strategy does not.\n",
      "## Results\n",
      md_table([dict(metric=k,value=v) for k,v in f.items() if not isinstance(v,dict)], ["metric","value"]),
      "\n## Trades\n",md_table([dict(metric=k,value=v) for k,v in m["trades"].items() if not isinstance(v,dict)],["metric","value"]),
      "\n## Predictions\n",md_table([dict(metric=k,value=v) for k,v in p.items() if not isinstance(v,dict)],["metric","value"]),
      "\n## Engineering\n",md_table([dict(metric=k,value=v) for k,v in e.items() if not isinstance(v,(dict,list))],["metric","value"]),
      "\nLatency distributions (milliseconds):\n",md_table([dict(metric=k,**e[k]) for k in ["prediction_roundtrip_batch_ms","inference_cycle_total_ms","warmup_cycle_total_ms","amortized_batch_ms_per_ticker","later_inference_ms"]],["metric","n","mean","median","p95","p99","max"]),
      "\nNon-overlapping stage totals:\n",md_table([dict(stage=k,seconds=v,percent_cycle_work=e["stage_percent_of_cycle_work"][k]) for k,v in e["stage_seconds"].items()],["stage","seconds","percent_cycle_work"]),
      "\nInference-only workload groups (has entry / has exit):\n",md_table([dict(group=g["group"],**g["cycle_ms"]) for g in e["work_groups"]],["group","n","mean","median","p95","p99"]),
      "\n## Risk and concentration\n",md_table([dict(metric=k,value=v) for k,v in r.items() if not isinstance(v,(dict,list))],["metric","value"]),
      "\nExposure/concentration distributions:\n",md_table([dict(metric=k,**r[k]) for k in ["exposure","inference_cycle_exposure","concurrent_tickers","invested_hhi"]],["metric","n","mean","median","p95","max"]),
      "\nFixed-fill cost arithmetic (not a strategy rerun; one-way cost applies to every buy and sell notional):\n",md_table(r["cost_sensitivity_fixed_fills"],["one_way_bps","hypothetical_pnl"]),
      "\n## Session-block uncertainty\n",md_table([dict(metric=k,**v) for k,v in {
        "mean_daily_strategy_return":f["mean_daily_return_bootstrap"],"mean_daily_excess_return":f["mean_daily_excess_return_bootstrap"],
        "mean_trade_pnl":m["trade_mean_pnl_bootstrap"],"brier_advantage_vs_sample_base":p["brier_advantage_bootstrap"],
        "volatility_mae_advantage_vs_metadata_mean":p["volatility_mae_advantage_bootstrap"]}.items()],
        ["metric","estimate","lower95","upper95","sessions","block_sessions","resamples"]),
      "\n"+m["bootstrap_limitations"]+" No subgroup is a separately validated strategy. No significance claim is made.\n",
      "\n## Coverage and reliability\n",md_table([dict(metric=k,value=v) for k,v in m["coverage"].items() if not isinstance(v,list)],["metric","value"]),
      "\n## Diagnostic subgroup tables\n\nReturns and rates are fractions, not percentages. Small groups are descriptive only; observations overlap across time and share market shocks.\n"]
    for name,rows in m["tables"].items():
        if name.startswith("trades"):
            columns=["group","trades","sessions","win_rate","total_pnl","mean_return","profit_factor","extended_trades"]
        elif name=="exposure_vs_drawdown":columns=["group","cycles","mean_drawdown","worst_drawdown"]
        elif name=="movement_by_volatility_quartile":columns=["group","labeled","positive_threshold_rate","negative_threshold_rate","positive_direction_rate","mean_fixed_target_return","classifier_auc"]
        else:columns=["group","predictions","labeled","labeled_sessions","positive_rate","mean_probability","mean_fixed_target_return","brier","auc","volatility_mae"]
        report.extend(["\n### "+name+"\n",md_table(rows,columns)])
    report.extend(["\n## Charts\n",*[f"![{name}]({name}.png)\n" for name in ["equity_drawdown","attribution_exposure","prediction_diagnostics","engineering"]],
        "\n## Definitions, calculations, and qualifications\n\nSee [METHODS.md](../../../analytics/METHODS.md) for exact field mappings and formulas, target timing, uncertainty and unit caveats. See [findings.md](findings.md) for interpretation and A/B/C classifications. See `metrics.json` for complete distributions and `*.jsonl` for every derived observation. `analysis_completion.json` binds output hashes to unchanged input hashes.\n"])
    (out/"report.md").write_text("\n".join(report),encoding="utf-8")


def resume_facts(m):
    e=m["engineering"]
    facts=[
      dict(metric="CPU batch prediction latency",value=dict(median_ms=e["prediction_roundtrip_batch_ms"]["median"],p95_ms=e["prediction_roundtrip_batch_ms"]["p95"]),
        calculation="numpy.quantile(predictionDurationNanos / 1e6, [0.5, 0.95]) on COMPLETED inference cycles only",
        denominator="2,773 inference requests; 15 tickers each; 41,595 ticker predictions",
        artifact_fields=["session.json: cycleLogs[].evaluation.inferenceStatus", "cycleLogs[].evaluation.inferenceBatchSize", "cycleLogs[].evaluation.predictionDurationNanos", "manifest.json: runtime and predictionService.runtime"],
        qualification="One local CPU backtest, serial calls, fixed batch size. Includes first inference; excludes 1,798 empty-window cycles. HTTP round trip includes mapping, models and parsing. Not standalone single-ticker or pure neural-kernel latency.",
        why_defensible="Every included observation and workload is logged; percentiles can be reproduced without rerunning models."),
      dict(metric="End-to-end historical throughput",value=dict(seconds=e["full_backtest_wall_seconds"],predictions_per_second=e["predictions_per_full_wall_second"]),
        calculation="completion.fullBacktestWallNanos / 1e9; 41,595 / resulting seconds",
        denominator="4,571 cycles, 2,773 inference batches, 41,595 predictions, 545 completed trades, 59 market sessions",
        artifact_fields=["completion.json: fullBacktestWallNanos, cycleCount", "session.json: cycleLogs and execution records"],
        qualification="Already-running Python service; excludes process startup, training and data download. Includes run setup/provenance, snapshot, execution and session serialization. One observed run, not a sustained load benchmark.",
        why_defensible="Explicit monotonic timing scope and exact workload; warm-up cycles are not counted as ticker predictions."),
      dict(metric="Auditable multi-ticker execution",value=m["integrity"],
        calculation="Join entries, exits and adjustments by tradeId; verify every entry closes once; reconcile quantities, fills, marked equity and terminal flat state",
        denominator="545 entries, 545 exits, 4,550 applied extensions, 4,571 cycle valuations plus initial/final valuation",
        artifact_fields=["session.json: successfullyOpenedTrades, successfulEntryExecutions, successfullyClosedTrades, successfulExitExecutions, appliedOpenTradeAdjustments", "portfolioAfterCycle, finalPortfolio, liquidationResult, remainingOpenTrades", "manifest.json: data.sha256, git.sourceFileSha256; completion.json: sessionSha256"],
        qualification="Historical paper execution with exact candle-open fills and no modeled costs. Zero failures describes this run, not production availability or future failure probability.",
        why_defensible="Stable IDs, cash/position reconciliation, exact snapshot prices, immutable inputs and matching checksums support independent audit."),
    ]
    return facts


def write_findings(out,m):
    # Interpretation is specific to this fixed run, not an automated search for favorable periods.
    f,e,p,r,tr=m["financial"],m["engineering"],m["prediction"],m["risk"],m["trades"]
    a=p["auc_session_block_uncertainty"]
    facts=resume_facts(m)
    write_json(out/"resume_candidates.json",facts)
    lines=["# Findings and presentation classification\n",
      f"This is a fixed 59-session exploratory evaluation, not strategy selection. Gross strategy return was **{100*f['total_return']:.3f}%**, versus **{100*f['spy_total_return']:.3f}%** for identically bounded SPY price-only buy-and-hold: **{f['excess_return_percentage_points']:.3f} percentage points**. Engineering and auditability are the strongest resume evidence.\n",
      "## A. Resume-candidate results\n\nThese are claim ingredients, not final resume bullets.\n"]
    for fact in facts:
        lines += [f"### {fact['metric']}\n", "- Exact metric: `"+json.dumps(clean(fact["value"]))+"`",
            *[f"- {name.replace('_',' ').capitalize()}: {fact[name]}" for name in ["calculation","denominator","qualification","why_defensible"]],
            "- Artifact fields: "+"; ".join(fact["artifact_fields"])+"\n"]
    lines += ["\n## B. Portfolio / README results\n",
      f"- **Modest gross profitability, with benchmark underperformance:** ${f['gross_pnl']:.2f} on ${f['initial_equity']:.0f}; {tr['wins']}/{tr['trades']} winning trades ({100*tr['win_rate']:.2f}%), profit factor {tr['profit_factor']:.3f}. This is descriptive, before costs. The session-block 95% interval for mean daily return spans {100*f['mean_daily_return_bootstrap']['lower95']:.4f}% to {100*f['mean_daily_return_bootstrap']['upper95']:.4f}%; it includes zero.",
      f"- **Lower observed drawdown with substantially less exposure:** strategy {100*f['max_drawdown']:.3f}% versus SPY {100*f['spy_max_drawdown']:.3f}%; mean cycle exposure {100*r['exposure']['mean']:.2f}%. SPY holds overnight; strategy does not. This is not a matched-risk comparison or proof of superior risk management.",
      f"- **Classifier discrimination:** ROC AUC {p['auc']:.4f}; 95% five-session-block interval [{a['lower95']:.4f}, {a['upper95']:.4f}] over {p['labeled']:,} labeled predictions across 59 sessions. Brier {p['brier']:.6f} versus {p['sample_base_rate_brier']:.6f} for an ex-post sample-prevalence constant ({100*p['brier_skill_vs_sample_base']:.2f}% relative improvement). Predictions are overlapping, not independent trials.",
      f"- **Important classifier caveat:** probability and predicted volatility have rank correlation {p['probability_volatility_spearman']:.3f}. Predicted volatility alone ranks the classifier event with AUC {a['volatility_score_auc']:.4f}; classifier minus volatility-score AUC is {a['paired_auc_advantage_over_volatility']:.4f} (block interval [{a['paired_advantage_lower95']:.4f}, {a['paired_advantage_upper95']:.4f}]). This is a diagnostic of saved scores, not a new strategy. A positive-return threshold becomes easier to exceed in either-direction volatile markets; ranking this event is not the same as predicting a profitable direction.",
      f"- **Volatility model contains useful descriptive signal after unit conversion:** predicted/realized correlation {p['volatility_pearson']:.3f}, MAE {p['volatility_mae']:.8f} return units, {100*p['volatility_mae_reduction_vs_metadata_mean']:.2f}% below the endpoint-metadata constant-mean baseline. Logged forecasts are in training units (target multiplied by 1,000); divide by 1,000 only for evaluation. The actual Java allocator used the logged scale unchanged. This prevents promoting correct volatility-calibrated live allocation from this run.",
      f"- **Trade behavior:** {tr['extended_trades']}/{tr['trades']} trades were extended; median holding {tr['holding_minutes']['median']:.0f} minutes versus the initial 30-minute plan. Non-extended trades: 88, 69.32% wins, $104.05 P&L; extended: 457, 49.23% wins, $76.39 P&L. Extension is conditional on subsequent signals: this comparison cannot establish that extensions help or hurt causally.",
      f"- **Concentration:** AMD contributes ${r['largest_ticker_pnl']:.2f}, leaving ${r['pnl_excluding_largest_ticker']:.2f} from other tickers. The five largest winners account for {100*r['top5_winners_share_gross_wins']:.2f}% of gross winning P&L but {100*r['top5_winners_share_net_pnl']:.2f}% of net P&L. Removing their accounting contributions leaves ${r['pnl_excluding_top5_winners']:.2f}; this is attribution, not a rerun without those trades.",
      "- **Subgroups are not a tuning recommendation:** AAPL and AMD contributed positively; TSLA was the largest negative contributor. COST (2 trades) and SPY (3) have 100% observed trade win rates, which are too small to promote. The highest probability bucket has only 61 labeled predictions and a negative mean fixed-target return. Tables retain every ticker, all observed probability/volatility buckets, their interaction, decisions, extensions, and zero-volume overlap.",
      f"- **Runtime is dominated by prediction:** prediction round trips total {e['prediction_seconds']:.3f}s; evaluation is {e['stage_percent_of_cycle_work']['evaluation_ms']:.2f}% of summed cycle work. First inference is {e['first_inference_ms']:.3f}ms and remains included in the headline latency distribution. Entry-work groups have similar median cycle latency (about 10ms); tails differ and include startup. All inference batches are size 15, so batch-size scaling cannot be inferred.",
      "- **Data limitation retained:** all 21 zero-volume candles are preserved. They occur in 420 prediction windows (361 labeled), across 372 cycles. Subgroup diagnostics are disclosed without deleting records or changing the strategy. Differences cannot establish a causal effect of bad volume.",
      "\n## C. Results not suitable for promotion\n",
      f"- Outperformance or statistically established trading alpha: SPY return was higher; daily excess-return and mean-trade-P&L block intervals include zero. The descriptive annualized daily Sharpe ({f['daily_sharpe_annualized_descriptive']:.2f}) comes from only 59 sessions and is unsuitable as a robust headline.",
      f"- Expected live profitability: linear break-even cost is only {r['linear_break_even_cost_bps']:.3f} bps per side. At 5 bps on each recorded fill, fixed-fill arithmetic gives ${r['cost_sensitivity_fixed_fills'][1]['hypothetical_pnl']:.2f}. This does not model cost-induced changes in capital or decisions and is not a rerun.",
      "- Claims of calibrated volatility-aware allocation: the serving/strategy path uses forecasts scaled by 1,000. No code or fills were corrected for this analysis. The model diagnostics use converted units; simulated trade results remain as executed.",
      "- High accuracy without class balance, high-confidence tiny subgroups, individual winning tickers, or extended/non-extended comparisons as validated strategies. The all-negative classifier baseline is already about 83.69% accurate. Many observations share labels, tickers and market conditions.",
      "- Sub-millisecond single-ticker latency: dividing a batch's latency by 15 is amortized work, not the response time of a single-ticker request. No production SLO, scaling law, concurrency claim, download-inclusive runtime or speedup over an unmeasured baseline is established.",
      "\n## Five strongest overall facts\n",
      f"1. A reproducible Java/Python CPU run processed 15 tickers, 41,595 ticker predictions and 545 completed trades in {e['full_backtest_wall_seconds']:.3f}s of recorded application time.",
      f"2. Full 15-ticker prediction round trips had {e['prediction_roundtrip_batch_ms']['median']:.3f}ms median and {e['prediction_roundtrip_batch_ms']['p95']:.3f}ms p95, using all 2,773 actual inference calls.",
      "3. The artifact supports a full audit: 545 round trips, 4,550 extensions, exact-price marked equity, no failed executions or stale marks, and a flat terminal portfolio.",
      f"4. Saved predictions contain measurable out-of-sample descriptive structure (classifier AUC {p['auc']:.3f}; converted volatility correlation {p['volatility_pearson']:.3f}), but volatility dependence and the serving-scale issue limit stronger claims.",
      f"5. The honest trading result is a small gross gain ({100*f['total_return']:.2f}%) with lower exposure/drawdown, but benchmark underperformance, concentration and very limited cost headroom—not evidence of a deployable trading edge.\n",
      "All claims are conditional on the documented execution assumptions, fixed ticker universe, 59 sessions, retained volume anomalies, and exact immutable run. No final resume bullet is written here.\n"]
    (out/"findings.md").write_text("\n".join(lines),encoding="utf-8")


if __name__=="__main__":
    main()
