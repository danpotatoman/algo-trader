import json
from pathlib import Path
import unittest

import numpy as np
import pandas as pd
from fastapi import HTTPException

from app.cnn_classification_volatility_v1_feature_transformer import rows_to_features
from app import model_server as server
from training.cnn_threshold_classification import build_examples as classification
from training.cnn_volatility_regression import build_examples as volatility


def sample_data(start="2026-05-18T13:30:00Z"):
    times = pd.date_range(start, periods=37, freq="5min")
    closes = 100 + np.arange(37) * 0.137 + np.sin(np.arange(37))
    return pd.DataFrame({
        "ticker": "AAPL", "timestamp": times.as_unit("s").asi8,
        "datetime": times, "open": closes - 0.2, "high": closes + 0.5,
        "low": closes - 0.7, "close": closes, "volume": 10000 + np.arange(37) * 13,
    })


def request_rows(df):
    rows = df.iloc[:31][["open", "high", "low", "close", "volume"]].to_dict("records")
    for row, timestamp in zip(rows, df["datetime"]):
        row["timestamp"] = timestamp.isoformat()
    return rows


class InferenceFeaturesTest(unittest.TestCase):
    def test_features_match_both_training_pipelines(self):
        for start in ("2026-05-18T13:30:00Z", "2026-01-05T14:30:00Z"):
            df = sample_data(start)
            actual = rows_to_features(request_rows(df))
            self.assertEqual(actual.shape, (30, 10))
            self.assertEqual(actual.dtype, np.float32)
            for pipeline in (classification, volatility):
                with self.subTest(start=start, pipeline=pipeline.__name__):
                    example = pipeline.build_single_example(pipeline.add_base_features(df), 30, 30)
                    self.assertIsNotNone(example)
                    np.testing.assert_allclose(actual.T, example[0], rtol=1e-6, atol=1e-7)
            self.assertAlmostEqual(actual[0, 5], df.close.iloc[1] / df.close.iloc[0] - 1, places=7)
            self.assertEqual(actual[-1, 3], 0.0)

    def test_context_only_affects_first_return(self):
        rows = request_rows(sample_data())
        original = rows_to_features(rows)
        rows[0]["close"] *= 0.9
        modified = rows_to_features(rows)
        self.assertNotEqual(original[0, 5], modified[0, 5])
        modified[0, 5] = original[0, 5]
        np.testing.assert_array_equal(original, modified)

    def test_single_and_batch_keep_model_shape_and_horizon(self):
        request = server.ClassificationVolatilityPredictRequest(ticker="AAPL", rows=request_rows(sample_data()))
        self.assertEqual(tuple(server.rows_to_cnn_tensor(request.rows).shape), (1, 10, 30))
        self.assertEqual(tuple(server.batch_rows_to_cnn_tensor([request, request]).shape), (2, 10, 30))
        single = server.predict_cnn_classification_volatility_v1(request)
        batch = server.predict_cnn_classification_volatility_v1_batch(
            server.ClassificationVolatilityBatchPredictRequest(batches=[request, request]))
        self.assertEqual(single.horizonMinutes, 30)
        self.assertEqual(len(batch.predictions), 2)
        for prediction in batch.predictions:
            self.assertEqual(prediction.horizonMinutes, 30)
            self.assertAlmostEqual(prediction.probability, single.probability, places=5)
            self.assertAlmostEqual(prediction.volatility, single.volatility, places=5)

    def test_wrong_row_counts_are_rejected(self):
        rows = [server.PredictionRow(**row) for row in request_rows(sample_data())]
        for invalid in ([], rows[:1], rows[:30], rows + [rows[-1]]):
            with self.subTest(count=len(invalid)):
                with self.assertRaises(HTTPException) as raised:
                    server.rows_to_cnn_tensor(invalid)
                self.assertEqual(raised.exception.status_code, 400)

    def test_java_configs_match_request_contract(self):
        root = Path(__file__).resolve().parents[2]
        for name in ("batch-cnn-v1", "cnn-with-volatility-v1"):
            config = json.loads((root / "config" / "endpoints" / f"{name}.json").read_text())
            self.assertEqual(config["numCandles"], server.CnnClassificationVolatilityV1Config.n_request_rows)


if __name__ == "__main__":
    unittest.main()
