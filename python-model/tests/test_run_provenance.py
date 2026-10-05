import hashlib
from pathlib import Path
import tempfile
import unittest
import torch
from app import model_server as server


class RunProvenanceTest(unittest.TestCase):
    def test_hash_and_cutoffs_describe_exact_checkpoint_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "test.pt"
            torch.save({"last_train_timestamp": 100, "last_val_timestamp": 200,
                        "thresholds": {"return_30m": 0.002},
                        "api_key": "must-not-appear"}, path)
            checkpoint, identity = server.load_checkpoint_identity(str(path), "test")
            self.assertEqual(identity["sha256"], hashlib.sha256(path.read_bytes()).hexdigest())
            self.assertEqual(identity["last_val_timestamp"], 200)
            self.assertEqual(identity["return_30m_threshold"], 0.002)
            self.assertNotIn("api_key", identity)
            path.write_bytes(b"changed after loading")
            self.assertNotEqual(identity["sha256"], hashlib.sha256(path.read_bytes()).hexdigest())
            self.assertEqual(checkpoint["last_train_timestamp"], 100)

    def test_metadata_and_batch_use_same_server_identity(self):
        metadata = server.provenance()
        self.assertEqual(len(metadata["models"]), 2)
        for model in metadata["models"]:
            self.assertEqual(len(model["sha256"]), 64)
            self.assertIn("modelId", model)
        response = server.predict_cnn_classification_volatility_v1_batch(
            server.ClassificationVolatilityBatchPredictRequest(batches=[]))
        self.assertEqual(response.serverInstanceId, metadata["serverInstanceId"])
        self.assertEqual(response.predictions, [])
        self.assertIn("app/model_server.py", metadata["sourceFileSha256"])


if __name__ == "__main__":
    unittest.main()
