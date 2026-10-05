import json
import sys
import socket
import threading
import time
import urllib.request
import unittest
from unittest.mock import patch

import torch
import uvicorn
from app import model_server as server
from app.volatility_units import require_target_scale, predict_return_fraction_volatility
from test_inference_features import sample_data, request_rows


class KnownModel:
    target_scale = 1000.0

    def __call__(self, inputs):
        return torch.ones((len(inputs), 1))


class KnownClassifier:
    def __call__(self, inputs):
        return torch.zeros((len(inputs), 1))  # sigmoid -> probability 0.5


def endpoint_probe():
    """Real route/serialization output also consumed by the Java integration test."""
    request = dict(ticker="AAPL", rows=request_rows(sample_data()))
    with patch.object(server, "classification_volatility_v1_volatility_model", KnownModel()), \
            patch.object(server, "classification_volatility_v1_classification_model", KnownClassifier()):
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            http = uvicorn.Server(uvicorn.Config(server.app, log_level="critical", lifespan="off"))
            thread = threading.Thread(target=http.run, kwargs={"sockets": [sock]}, daemon=True)
            thread.start()
            try:
                deadline = time.monotonic() + 10
                while not http.started:
                    if not thread.is_alive() or time.monotonic() > deadline:
                        raise RuntimeError("Unit-contract HTTP server failed to start")
                    time.sleep(.01)
                base = f"http://127.0.0.1:{sock.getsockname()[1]}"
                def call(path, body=None):
                    payload = None if body is None else json.dumps(body).encode()
                    req = urllib.request.Request(base + path, data=payload,
                                                 headers={"Content-Type": "application/json"})
                    with urllib.request.urlopen(req, timeout=10) as response:
                        return json.load(response)
                return dict(single=call("/predict/cnn-classification-volatility-v1", request),
                            batch=call("/predict/cnn-classification-volatility-v1/batch",
                                       {"batches": [request, dict(request, ticker="MSFT")]}),
                            provenance=call("/provenance"))
            finally:
                http.should_exit = True
                thread.join(timeout=10)
                if thread.is_alive():
                    raise RuntimeError("Unit-contract HTTP server did not stop")


class VolatilityUnitsTest(unittest.TestCase):
    def test_single_and_batch_http_responses_are_canonical(self):
        result = endpoint_probe()
        self.assertAlmostEqual(result["single"]["volatility"], .001, places=9)
        for prediction in result["batch"]["predictions"]:
            self.assertEqual(prediction["volatility"], result["single"]["volatility"])
        contract = result["provenance"]["volatilityOutputContract"]
        self.assertEqual(contract["units"], "return_fraction")
        self.assertTrue(contract["inverseTargetScalingApplied"])

    def test_conversion_uses_metadata_not_a_constant(self):
        model = KnownModel()
        model.target_scale = 200.0
        self.assertAlmostEqual(predict_return_fraction_volatility(model, [0]).item(), .005, places=8)

    def test_invalid_metadata_fails_during_model_load(self):
        for scale in [None, 0, -1, float("nan"), float("inf"), True, "1000"]:
            checkpoint = {} if scale is None else {"target_scale": scale}
            with self.subTest(scale=scale):
                with self.assertRaisesRegex(ValueError, "target_scale.*finite and positive"):
                    require_target_scale(checkpoint)
                with patch.object(server, "load_checkpoint_identity", return_value=(checkpoint, {})):
                    with self.assertRaisesRegex(ValueError, "target_scale"):
                        server.load_volatility_regression_model("cnn-volatility-v1")

    def test_loaded_checkpoint_and_real_prediction_use_same_scale(self):
        model = server.classification_volatility_v1_volatility_model
        self.assertEqual(model.target_scale, model.run_identity["target_scale"])
        request = server.ClassificationVolatilityPredictRequest(ticker="AAPL", rows=request_rows(sample_data()))
        with torch.no_grad():
            expected = model(server.rows_to_cnn_tensor(request.rows)).item() / model.target_scale
        self.assertAlmostEqual(server.predict_cnn_classification_volatility_v1(request).volatility, expected, places=9)


if __name__ == "__main__":
    if "--emit-probe" in sys.argv:
        probe = endpoint_probe()
        # Keep subprocess output below pipe capacity; these are the actual API fields.
        probe["provenance"].pop("sourceFileSha256")
        probe["provenance"].pop("runtime")
        print(json.dumps(probe))
    else:
        unittest.main()
