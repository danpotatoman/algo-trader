import importlib.util
import json
import copy
from pathlib import Path
import sys
import unittest

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "python-model"))
spec = importlib.util.spec_from_file_location("analysis", ROOT / "analytics/analyze_showcase.py")
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)


class AnalyticsTests(unittest.TestCase):
    def test_legacy_and_canonical_volatility_do_not_double_convert(self):
        manifest = json.loads((ROOT / "data/logs/runs" / analysis.RUN_ID / "manifest.json").read_text())
        self.assertEqual(1.0 / analysis.volatility_divisor(manifest), .001)
        corrected = copy.deepcopy(manifest)
        corrected["runId"] = "future-run"
        corrected["predictionService"]["volatilityOutputContract"] = {
            "version": "volatility-return-fraction-v1", "units": "return_fraction",
            "inverseTargetScalingApplied": True}
        self.assertEqual(.001 / analysis.volatility_divisor(corrected), .001)
        del corrected["predictionService"]["volatilityOutputContract"]
        with self.assertRaisesRegex(ValueError, "Missing volatility"):
            analysis.volatility_divisor(corrected)
        corrected["predictionService"]["volatilityOutputContract"] = {"version": "unknown"}
        with self.assertRaisesRegex(ValueError, "Unsupported volatility"):
            analysis.volatility_divisor(corrected)
        for bad in [None, 0, -1, float("nan"), float("inf"), True]:
            legacy = copy.deepcopy(manifest)
            next(m for m in legacy["predictionService"]["models"] if "target_scale" in m)["target_scale"] = bad
            with self.assertRaisesRegex(ValueError, "target_scale"):
                analysis.volatility_divisor(legacy)

    def test_drawdown_retains_initial_capital_anchor(self):
        np.testing.assert_allclose(analysis.drawdown([90, 110, 99, 121], 100), [-.1, 0, -.1, 0])

    def test_auc_ties_and_order(self):
        self.assertEqual(analysis.auc([0,1], [.5,.5]), .5)
        self.assertEqual(analysis.auc([0,1,0,1], [.1,.9,.2,.8]), 1)
        self.assertEqual(analysis.auc([0,1], [.9,.1]), 0)

    def test_target_matches_original_training_builders(self):
        from training.cnn_threshold_classification.build_examples import add_base_features, build_single_example
        from training.cnn_volatility_regression.build_examples import build_single_example as volatility_example
        stamps = np.arange(37)*300 + 1783431000
        close = 100*np.exp(np.arange(37)*.001 + .0002*np.sin(np.arange(37)))
        frame = pd.DataFrame(dict(ticker="TEST", timestamp=stamps, datetime=pd.to_datetime(stamps,unit="s",utc=True),
            open=close, high=close+1, low=close-1, close=close, volume=1000))
        frame = add_base_features(frame)
        _, y, meta = build_single_example(frame,30,30)
        _, v, _ = volatility_example(frame,30,30)
        data = {("TEST",int(stamp)):(price,price+1,price-1,price,1000) for stamp,price in zip(stamps,close)}
        outcome = analysis.target_outcome(data,"TEST",int(stamps[31]))
        self.assertAlmostEqual(outcome["target_return"],meta["return_30m"])
        self.assertEqual(outcome["target_return"]>=.002,bool(y[0]))
        self.assertAlmostEqual(outcome["realized_volatility"],float(v[0]),places=10)
        self.assertEqual(outcome["target_realized_at"],stamps[31]+1800)

    def test_incomplete_future_is_not_stitched_or_imputed(self):
        data = {("T",stamp):(100,100,100,100,0) for stamp in range(0,1801,300)}
        self.assertEqual(analysis.target_outcome(data,"T",300)["realized_volatility"],0)
        del data["T",900]
        self.assertIsNone(analysis.target_outcome(data,"T",300))

    def test_session_bootstrap_preserves_constant_and_is_deterministic(self):
        first = analysis.block_interval([2]*10,[4]*10,draws=100)
        self.assertEqual(first,analysis.block_interval([2]*10,[4]*10,draws=100))
        self.assertEqual(first["lower95"],.5)
        self.assertEqual(first["upper95"],.5)

    def test_weighted_auc_bootstrap_and_paired_reference(self):
        result = analysis.auc_block_interval(np.array([0,1]*10),np.array([.1,.9]*10),
            np.repeat(np.arange(10),2),np.array([.1,.9]*10),draws=100)
        self.assertEqual(result["estimate"],1)
        self.assertEqual(result["lower95"],1)
        self.assertEqual(result["paired_advantage_upper95"],0)


if __name__ == "__main__":
    unittest.main()
