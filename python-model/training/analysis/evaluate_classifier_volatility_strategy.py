from pathlib import Path

import numpy as np
import torch
from torch.utils.data import TensorDataset, DataLoader
import matplotlib.pyplot as plt

from training.load_data import load_five_minute_dataframe
from training.split_data import chronological_split

from training.cnn_threshold_classification.build_examples import (
    add_base_features,
    build_classification_examples,
    TARGET_COLUMN,
)
from training.cnn_threshold_classification.model import CNNThresholdClassificationModel

from training.cnn_volatility_regression.build_examples import (
    build_regression_examples,
)
from training.cnn_volatility_regression.model import CNNVolatilityRegressionModel


WINDOW_SIZE = 30
BATCH_SIZE = 128

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DB_PATH = PROJECT_ROOT / "data" / "ohlcv.db"

CLASSIFICATION_MODEL_PATH = (
    PROJECT_ROOT
    / "python-model"
    / "saved_models"
    / "cnn-threshold-classification-v1.pt"
)

VOLATILITY_MODEL_PATH = (
    PROJECT_ROOT
    / "python-model"
    / "saved_models"
    / "cnn-volatility-v1.pt"
)


CLASSIFICATION_THRESHOLDS = [0.10, 0.15, 0.20, 0.25, 0.30]
VOLATILITY_THRESHOLDS = [
    0.0001,
    0.0002,
    0.0003,
    0.0004,
    0.0005,
    0.00075,
    0.0010,
    0.0015,
    0.0020,
]


def collect_classification_probabilities(model, data_loader, device):
    model.eval()

    probabilities = []

    with torch.no_grad():
        for X_batch, in data_loader:
            X_batch = X_batch.to(device)

            logits = model(X_batch)
            probs = torch.sigmoid(logits).cpu().numpy()

            probabilities.append(probs)

    return np.vstack(probabilities)


def collect_volatility_predictions(model, data_loader, device):
    model.eval()

    predictions = []

    with torch.no_grad():
        for X_batch, in data_loader:
            X_batch = X_batch.to(device)

            pred = model(X_batch).cpu().numpy()
            predictions.append(pred)

    return np.vstack(predictions)


def print_histogram(values, bins=20):
    values = np.asarray(values).reshape(-1)

    counts, edges = np.histogram(values, bins=bins)

    max_count = counts.max()

    print("\nHistogram")
    print("=" * 60)

    for i in range(len(counts)):
        left = edges[i]
        right = edges[i + 1]

        bar_length = int(50 * counts[i] / max_count)
        bar = "#" * bar_length

        print(
            f"{left:10.8f} - {right:10.8f} | "
            f"{counts[i]:6d} | {bar}"
        )

def assert_metadata_aligned(classification_metadata, volatility_metadata):
    if len(classification_metadata) != len(volatility_metadata):
        raise ValueError(
            "Classification and volatility metadata lengths do not match: "
            f"{len(classification_metadata)} != {len(volatility_metadata)}"
        )

    for i, (class_meta, vol_meta) in enumerate(
        zip(classification_metadata, volatility_metadata)
    ):
        class_key = (
            class_meta["ticker"],
            class_meta["window_end_timestamp"],
            class_meta["target_30m_timestamp"],
        )
        vol_key = (
            vol_meta["ticker"],
            vol_meta["window_end_timestamp"],
            vol_meta["target_30m_timestamp"],
        )

        if class_key != vol_key:
            raise ValueError(
                f"Metadata mismatch at index {i}: "
                f"classification={class_key}, volatility={vol_key}"
            )

def plot_probability_vs_volatility(
    probabilities,
    predicted_volatility,
):
    probs = np.asarray(probabilities).reshape(-1)
    vols = np.asarray(predicted_volatility).reshape(-1)

    correlation = np.corrcoef(probs, vols)[0, 1]

    plt.figure(figsize=(10, 6))
    plt.scatter(
        probs,
        vols,
        s=8,
        alpha=0.25,
    )

    plt.xlabel("Classifier Probability")
    plt.ylabel("Predicted Volatility (30m)")
    plt.title(
        f"Classifier Probability vs Predicted Volatility\n"
        f"Correlation = {correlation:.4f}"
    )

    plt.grid(True, alpha=0.3)
    plt.tight_layout()

    print(
        f"\nCorrelation between classifier probability "
        f"and predicted volatility: {correlation:.4f}"
    )

    plt.show()

def print_distribution(name, values):
    values = np.asarray(values).reshape(-1)

    print(f"\n{name}")
    print("=" * len(name))

    if len(values) == 0:
        print("No examples.")
        return

    print(f"Count: {len(values)}")
    print(f"Mean:  {np.mean(values):.8f} ({np.mean(values) * 10_000:.4f} bps)")
    print(f"Std:   {np.std(values):.8f} ({np.std(values) * 10_000:.4f} bps)")
    print(f"Min:   {np.min(values):.8f} ({np.min(values) * 10_000:.4f} bps)")
    print(f"Max:   {np.max(values):.8f} ({np.max(values) * 10_000:.4f} bps)")

    print("\nPercentiles:")
    print(f"{'pct':>5} {'raw':>14} {'bps':>12}")
    print("-" * 33)

    for pct in [1, 5, 10, 25, 50, 75, 90, 95, 99]:
        raw = np.percentile(values, pct)
        print(f"{pct:>5} {raw:>14.8f} {raw * 10_000:>12.4f}")


def print_volatility_distribution_by_classifier_threshold(
    probabilities,
    predicted_volatility,
):
    probs = probabilities.reshape(-1)
    pred_vol = predicted_volatility.reshape(-1)

    for class_threshold in CLASSIFICATION_THRESHOLDS:
        mask = probs >= class_threshold
        filtered_vol = pred_vol[mask]

        print_distribution(
            f"Predicted volatility when classifier probability >= {class_threshold:.2f}",
            filtered_vol,
        )


def plot_probability_bins_vs_volatility(
    probabilities,
    predicted_volatility,
    num_bins=20,
):
    probs = np.asarray(probabilities).reshape(-1)
    vols = np.asarray(predicted_volatility).reshape(-1)

    edges = np.linspace(0, 1, num_bins + 1)
    centers = []
    mean_vols = []

    for i in range(num_bins):
        mask = (probs >= edges[i]) & (probs < edges[i + 1])

        if np.any(mask):
            centers.append((edges[i] + edges[i + 1]) / 2)
            mean_vols.append(np.mean(vols[mask]))

    plt.figure(figsize=(10, 6))
    plt.plot(centers, mean_vols, marker="o")

    plt.xlabel("Classifier Probability")
    plt.ylabel("Average Predicted Volatility")
    plt.title("Average Volatility by Probability Bucket")

    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.show()

def print_strategy_grid(
    probabilities,
    predicted_volatility,
    actual_classification_labels,
):
    probs = probabilities.reshape(-1)
    pred_vol = predicted_volatility.reshape(-1)
    actual = actual_classification_labels.reshape(-1)

    total_count = len(probs)

    print("\nStrategy grid")
    print("=" * 80)
    print(
        f"{'conf':>6} "
        f"{'vol_thr':>10} "
        f"{'vol_bps':>9} "
        f"{'trades':>8} "
        f"{'trade%':>8} "
        f"{'hit_rate':>10} "
        f"{'avg_prob':>10} "
        f"{'avg_vol':>12}"
    )
    print("-" * 80)

    for class_threshold in CLASSIFICATION_THRESHOLDS:
        for vol_threshold in VOLATILITY_THRESHOLDS:
            mask = (probs >= class_threshold) & (pred_vol <= vol_threshold)
            trades = int(np.sum(mask))

            if trades > 0:
                trade_rate = trades / total_count
                hit_rate = np.mean(actual[mask])
                avg_prob = np.mean(probs[mask])
                avg_vol = np.mean(pred_vol[mask])
            else:
                trade_rate = 0.0
                hit_rate = np.nan
                avg_prob = np.nan
                avg_vol = np.nan

            print(
                f"{class_threshold:>6.2f} "
                f"{vol_threshold:>10.6f} "
                f"{vol_threshold * 10_000:>9.3f} "
                f"{trades:>8} "
                f"{trade_rate * 100:>7.2f}% "
                f"{hit_rate:>10.4f} "
                f"{avg_prob:>10.4f} "
                f"{avg_vol:>12.8f}"
            )

        print("-" * 80)


def main():
    print("Loading data...")
    df = load_five_minute_dataframe(DB_PATH)
    df = add_base_features(df)

    print("Building classification examples...")
    X_class, y_class, metadata_class = build_classification_examples(
        df,
        window_size=WINDOW_SIZE,
    )

    print("Building volatility examples...")
    X_vol, y_vol, metadata_vol = build_regression_examples(
        df,
        window_size=WINDOW_SIZE,
    )

    (
        X_class_train,
        y_class_train,
        metadata_class_train,
        X_class_val,
        y_class_val,
        metadata_class_val,
        X_class_test,
        y_class_test,
        metadata_class_test,
    ) = chronological_split(X_class, y_class, metadata_class)

    (
        X_vol_train,
        y_vol_train,
        metadata_vol_train,
        X_vol_val,
        y_vol_val,
        metadata_vol_val,
        X_vol_test,
        y_vol_test,
        metadata_vol_test,
    ) = chronological_split(X_vol, y_vol, metadata_vol)

    assert_metadata_aligned(metadata_class_test, metadata_vol_test)

    print("\nDataset shapes:")
    print(f"X_class_train: {X_class_train.shape}, y_class_train: {y_class_train.shape}")
    print(f"X_class_train:   {X_class_train.shape}, y_vol_train:   {y_vol_train.shape}")

    print("\nTest period:")
    print(f"First test example: {metadata_class_test[0]['window_end_datetime']}")
    print(f"Last test example:  {metadata_class_test[-1]['window_end_datetime']}")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\nUsing device: {device}")

    class_dataset = TensorDataset(torch.from_numpy(X_class_train))
    vol_dataset = TensorDataset(torch.from_numpy(X_class_train))

    class_loader = DataLoader(
        class_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
    )

    vol_loader = DataLoader(
        vol_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
    )

    print("\nLoading classification model...")
    class_checkpoint = torch.load(CLASSIFICATION_MODEL_PATH, map_location=device)

    class_model = CNNThresholdClassificationModel(
        input_channels=class_checkpoint["input_channels"],
        output_size=class_checkpoint["output_size"],
    ).to(device)

    class_model.load_state_dict(class_checkpoint["model_state_dict"])

    print("Loading volatility model...")
    vol_checkpoint = torch.load(VOLATILITY_MODEL_PATH, map_location=device)

    vol_model = CNNVolatilityRegressionModel(
        input_channels=vol_checkpoint["input_channels"],
        output_size=vol_checkpoint["output_size"],
    ).to(device)

    vol_model.load_state_dict(vol_checkpoint["model_state_dict"])

    print("\nCollecting model outputs...")
    probabilities = collect_classification_probabilities(
        class_model,
        class_loader,
        device,
    )

    predicted_volatility = collect_volatility_predictions(
        vol_model,
        vol_loader,
        device,
    )

    target_scale = vol_checkpoint.get("target_scale", 1.0)
    predicted_volatility = predicted_volatility / target_scale

    print(f"\nClassification target: {TARGET_COLUMN}")
    print(f"Actual positive rate: {np.mean(y_class_train[:, 0]):.4f}")
    print(f"Average predicted probability: {np.mean(probabilities[:, 0]):.4f}")

    print_distribution(
        "Predicted volatility over all test examples",
        predicted_volatility[:, 0],
    )

    print_volatility_distribution_by_classifier_threshold(
        probabilities[:, 0],
        predicted_volatility[:, 0],
    )

    print_strategy_grid(
        probabilities[:, 0],
        predicted_volatility[:, 0],
        y_class_train[:, 0],
    )

    confidence_threshold = 0.25

    mask = probabilities >= confidence_threshold

    filtered_volatility = predicted_volatility[mask]

    print(
        f"\nPredicted volatility histogram "
        f"(classifier probability >= {confidence_threshold})"
    )
    print(f"Examples: {len(filtered_volatility)}")

    print_histogram(filtered_volatility)

    plot_probability_vs_volatility(
        probabilities[:, 0],
        predicted_volatility[:, 0],
    )

    plot_probability_bins_vs_volatility(probabilities[:, 0],
        predicted_volatility[:, 0],)


if __name__ == "__main__":
    main()