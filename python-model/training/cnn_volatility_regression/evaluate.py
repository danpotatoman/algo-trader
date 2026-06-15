from pathlib import Path

import numpy as np
import torch
from torch.utils.data import TensorDataset, DataLoader

from training.load_data import load_five_minute_dataframe
from training.cnn_volatility_regression.build_examples import (
    add_base_features,
    build_regression_examples,
)
from training.split_data import chronological_split
from training.cnn_volatility_regression.model import CNNRegressionModel


WINDOW_SIZE = 30
BATCH_SIZE = 128

TARGET_COLUMNS = [
    "volatility_30m",
]

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DB_PATH = PROJECT_ROOT / "data" / "ohlcv.db"
MODEL_PATH = PROJECT_ROOT / "python-model" / "saved_models" / "cnn-volatility-v1.pt"


def collect_predictions(model, data_loader, device):
    model.eval()

    predictions = []
    actuals = []

    with torch.no_grad():
        for X_batch, y_batch in data_loader:
            X_batch = X_batch.to(device)

            pred = model(X_batch).cpu().numpy()

            predictions.append(pred)
            actuals.append(y_batch.numpy())

    return np.vstack(predictions), np.vstack(actuals)


def print_basic_metrics(pred, actual):
    print("\nVolatility metrics:")

    error = pred[:, 0] - actual[:, 0]

    mae = np.mean(np.abs(error))
    mse = np.mean(error ** 2)
    rmse = np.sqrt(mse)

    actual_std = np.std(actual[:, 0])
    pred_std = np.std(pred[:, 0])

    if actual_std > 0 and pred_std > 0:
        corr = np.corrcoef(pred[:, 0], actual[:, 0])[0, 1]
    else:
        corr = np.nan

    print(f"\nvolatility_30m")
    print(f"  MAE:        {mae:.8f}")
    print(f"  RMSE:       {rmse:.8f}")
    print(f"  Corr:       {corr:.4f}")
    print(f"  Actual avg: {np.mean(actual[:, 0]):.8f}")
    print(f"  Pred avg:   {np.mean(pred[:, 0]):.8f}")
    print(f"  Actual std: {actual_std:.8f}")
    print(f"  Pred std:   {pred_std:.8f}")


def print_baseline_comparison(pred, actual, y_train):
    print("\nBaseline comparison:")

    train_vol_mean = np.mean(y_train[:, 0])
    baseline = np.full_like(actual, train_vol_mean)

    model_mae = np.mean(np.abs(pred[:, 0] - actual[:, 0]))
    baseline_mae = np.mean(np.abs(baseline[:, 0] - actual[:, 0]))

    improvement = (
        (baseline_mae - model_mae) / baseline_mae * 100
        if baseline_mae != 0
        else np.nan
    )

    print(f"\nvolatility_30m")
    print(f"  Model MAE:    {model_mae:.8f}")
    print(f"  Baseline MAE: {baseline_mae:.8f}")
    print(f"  Improvement:  {improvement:.2f}%")


def print_volatility_buckets(pred, actual, num_buckets=5):
    print("\nPredicted volatility bucket test:")

    pred_vol = pred[:, 0]
    actual_vol = actual[:, 0]

    sorted_indices = np.argsort(pred_vol)
    buckets = np.array_split(sorted_indices, num_buckets)

    for bucket_num, indices in enumerate(buckets, start=1):
        avg_pred = np.mean(pred_vol[indices])
        avg_actual = np.mean(actual_vol[indices])

        print(
            f"Bucket {bucket_num}: "
            f"avg_pred={avg_pred:.8f}, "
            f"avg_actual={avg_actual:.8f}, "
            f"count={len(indices)}"
        )


def main():
    print("Loading data...")
    df = load_five_minute_dataframe(DB_PATH)
    df = add_base_features(df)

    print("Building examples...")
    X, y, metadata = build_regression_examples(
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

    model = CNNRegressionModel(
        input_channels=checkpoint["input_channels"],
        output_size=checkpoint["output_size"],
    ).to(device)

    model.load_state_dict(checkpoint["model_state_dict"])

    pred, actual = collect_predictions(model, test_loader, device)

    target_scale = checkpoint.get("target_scale", 1.0)
    pred = pred / target_scale

    print_basic_metrics(pred, actual)
    print_baseline_comparison(pred, actual, y_train)
    print_volatility_buckets(pred, actual)


if __name__ == "__main__":
    main()