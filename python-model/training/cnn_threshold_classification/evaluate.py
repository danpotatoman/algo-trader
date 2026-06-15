from pathlib import Path

import numpy as np
import torch
from torch.utils.data import TensorDataset, DataLoader

from training.load_data import load_five_minute_dataframe
from training.split_data import chronological_split
from training.cnn_threshold_classification.build_examples import (
    add_base_features,
    build_classification_examples,
    TARGET_COLUMNS,
)
from training.cnn_threshold_classification.model import CNNClassificationModel


WINDOW_SIZE = 30
BATCH_SIZE = 128

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DB_PATH = PROJECT_ROOT / "data" / "ohlcv.db"
MODEL_PATH = PROJECT_ROOT / "python-model" / "saved_models" / "cnn-threshold-classification-v1.pt"


def collect_probabilities(model, data_loader, device):
    model.eval()

    probabilities = []
    actuals = []

    with torch.no_grad():
        for X_batch, y_batch in data_loader:
            X_batch = X_batch.to(device)

            logits = model(X_batch)
            probs = torch.sigmoid(logits).cpu().numpy()

            probabilities.append(probs)
            actuals.append(y_batch.numpy())

    return np.vstack(probabilities), np.vstack(actuals)


def binary_metrics(probs, actual, threshold=0.5):
    pred = (probs >= threshold).astype(np.float32)

    tp = np.sum((pred == 1) & (actual == 1))
    tn = np.sum((pred == 0) & (actual == 0))
    fp = np.sum((pred == 1) & (actual == 0))
    fn = np.sum((pred == 0) & (actual == 1))

    accuracy = (tp + tn) / max(tp + tn + fp + fn, 1)
    precision = tp / max(tp + fp, 1)
    recall = tp / max(tp + fn, 1)
    f1 = (
        2 * precision * recall / max(precision + recall, 1e-12)
    )

    positive_rate = np.mean(actual)
    predicted_positive_rate = np.mean(pred)

    return {
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "positive_rate": positive_rate,
        "predicted_positive_rate": predicted_positive_rate,
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
    }


def print_threshold_sweep(probs, actual):
    print("\nClassification metrics across probability thresholds:")

    thresholds = np.arange(0.05, 0.55, 0.05)

    for i, target in enumerate(TARGET_COLUMNS):
        print(f"\n{'=' * 80}")
        print(target)
        print(f"{'=' * 80}")

        base_rate = np.mean(actual[:, i])

        print(f"Actual positive rate: {base_rate:.4f}\n")

        header = (
            f"{'thr':>5} "
            f"{'pred%':>8} "
            f"{'prec':>8} "
            f"{'recall':>8} "
            f"{'f1':>8} "
            f"{'acc':>8}"
        )

        print(header)
        print("-" * len(header))

        for threshold in thresholds:
            metrics = binary_metrics(
                probs[:, i],
                actual[:, i],
                threshold=threshold,
            )

            print(
                f"{threshold:>5.2f} "
                f"{metrics['predicted_positive_rate']:>8.4f} "
                f"{metrics['precision']:>8.4f} "
                f"{metrics['recall']:>8.4f} "
                f"{metrics['f1']:>8.4f} "
                f"{metrics['accuracy']:>8.4f}"
            )

        print(
            f"\nAverage predicted probability: "
            f"{np.mean(probs[:, i]):.4f}"
        )
        print(
            f"Predicted probability std: "
            f"{np.std(probs[:, i]):.4f}"
        )


def print_probability_buckets(probs, actual, num_buckets=5):
    print("\nProbability bucket tests:")

    for i, target in enumerate(TARGET_COLUMNS):
        print(f"\n{target}")

        target_probs = probs[:, i]
        target_actual = actual[:, i]

        sorted_indices = np.argsort(target_probs)
        buckets = np.array_split(sorted_indices, num_buckets)

        for bucket_num, indices in enumerate(buckets, start=1):
            avg_prob = np.mean(target_probs[indices])
            actual_positive_rate = np.mean(target_actual[indices])

            print(
                f"  Bucket {bucket_num}: "
                f"avg_prob={avg_prob:.4f}, "
                f"actual_positive_rate={actual_positive_rate:.4f}, "
                f"count={len(indices)}"
            )


def print_top_probability_slices(probs, actual):
    print("\nTop probability slice tests:")

    for i, target in enumerate(TARGET_COLUMNS):
        print(f"\n{target}")

        target_probs = probs[:, i]
        target_actual = actual[:, i]

        sorted_indices = np.argsort(target_probs)[::-1]

        for fraction in [0.01, 0.05, 0.10, 0.20]:
            n = max(int(len(sorted_indices) * fraction), 1)
            top_indices = sorted_indices[:n]

            avg_prob = np.mean(target_probs[top_indices])
            hit_rate = np.mean(target_actual[top_indices])

            print(
                f"  Top {int(fraction * 100):>2}%: "
                f"avg_prob={avg_prob:.4f}, "
                f"hit_rate={hit_rate:.4f}, "
                f"count={n}"
            )


def main():
    print("Loading data...")
    df = load_five_minute_dataframe(DB_PATH)
    df = add_base_features(df)

    print("Building classification examples...")
    X, y, metadata = build_classification_examples(
        df,
        window_size=WINDOW_SIZE,
    )

    (
        X_train,
        y_train,
        metadata_train,
        X_val,
        y_val,
        metadata_val,
        X_test,
        y_test,
        metadata_test,
    ) = chronological_split(X, y, metadata)

    print("\nDataset shapes:")
    print(f"X_train: {X_train.shape}, y_train: {y_train.shape}")
    print(f"X_val:   {X_val.shape}, y_val:   {y_val.shape}")
    print(f"X_test:  {X_test.shape}, y_test:  {y_test.shape}")

    print("\nTest period:")
    print(f"First test example: {metadata_test[0]['window_end_datetime']}")
    print(f"Last test example:  {metadata_test[-1]['window_end_datetime']}")

    test_dataset = TensorDataset(
        torch.from_numpy(X_test),
        torch.from_numpy(y_test),
    )

    test_loader = DataLoader(
        test_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
    )

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\nUsing device: {device}")

    checkpoint = torch.load(MODEL_PATH, map_location=device)

    model = CNNClassificationModel(
        input_channels=checkpoint["input_channels"],
        output_size=checkpoint["output_size"],
    ).to(device)

    model.load_state_dict(checkpoint["model_state_dict"])

    probs, actual = collect_probabilities(model, test_loader, device)

    print_threshold_sweep(probs, actual)
    print_probability_buckets(probs, actual)
    print_top_probability_slices(probs, actual)


if __name__ == "__main__":
    main()