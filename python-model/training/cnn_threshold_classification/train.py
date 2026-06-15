from pathlib import Path

import torch
from torch.utils.data import TensorDataset, DataLoader

from training.load_data import load_five_minute_dataframe
from training.split_data import chronological_split
from training.cnn_threshold_classification.build_examples import (
    add_base_features,
    build_classification_examples,
    FEATURE_COLUMNS,
    TARGET_COLUMNS,
    RETURN_5M_THRESHOLD,
    RETURN_10M_THRESHOLD,
    RETURN_30M_THRESHOLD,
)
from training.cnn_threshold_classification.model import CNNClassificationModel


WINDOW_SIZE = 30
BATCH_SIZE = 128
EPOCHS = 30
LEARNING_RATE = 1e-3
WEIGHT_DECAY = 1e-4

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DB_PATH = PROJECT_ROOT / "data" / "ohlcv.db"
MODEL_OUT = PROJECT_ROOT / "python-model" / "saved_models" / "cnn-threshold-classification-v1.pt"


def main() -> None:
    print("Loading data...")
    df = load_five_minute_dataframe(DB_PATH)
    df = add_base_features(df)

    print("Building classification examples...")
    X, y, metadata = build_classification_examples(
        df,
        window_size=WINDOW_SIZE,
    )

    print("\nOverall positive rates:")
    for i, target in enumerate(TARGET_COLUMNS):
        print(f"{target}: {y[:, i].mean():.4f}")

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

    print("\nSplit positive rates:")
    for split_name, split_y in [
        ("train", y_train),
        ("val", y_val),
        ("test", y_test),
    ]:
        print(f"\n{split_name}:")
        for i, target in enumerate(TARGET_COLUMNS):
            print(f"  {target}: {split_y[:, i].mean():.4f}")

    print("\nSplit boundaries:")
    print(
        f"Latest timestamp used by training labels: "
        f"{metadata_train[-1]['target_30m_timestamp']}"
    )
    print(f"First validation example: {metadata_val[0]['window_end_datetime']}")
    print(f"First test example:       {metadata_test[0]['window_end_datetime']}")

    train_dataset = TensorDataset(
        torch.from_numpy(X_train),
        torch.from_numpy(y_train),
    )

    val_dataset = TensorDataset(
        torch.from_numpy(X_val),
        torch.from_numpy(y_val),
    )

    test_dataset = TensorDataset(
        torch.from_numpy(X_test),
        torch.from_numpy(y_test),
    )

    train_loader = DataLoader(
        train_dataset,
        batch_size=BATCH_SIZE,
        shuffle=True,
    )

    val_loader = DataLoader(
        val_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
    )

    test_loader = DataLoader(
        test_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
    )

    print("\nDataLoaders created:")
    print(f"train batches: {len(train_loader)}")
    print(f"val batches:   {len(val_loader)}")
    print(f"test batches:  {len(test_loader)}")

    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\nUsing device: {device}")

    model = CNNClassificationModel(
        input_channels=X_train.shape[1],
        output_size=y_train.shape[1],
    ).to(device)

    loss_fn = torch.nn.BCEWithLogitsLoss()

    optimizer = torch.optim.AdamW(
        model.parameters(),
        lr=LEARNING_RATE,
        weight_decay=WEIGHT_DECAY,
    )

    best_val_loss = float("inf")

    for epoch in range(1, EPOCHS + 1):
        model.train()
        train_loss_total = 0.0

        for X_batch, y_batch in train_loader:
            X_batch = X_batch.to(device)
            y_batch = y_batch.to(device)

            logits = model(X_batch)
            loss = loss_fn(logits, y_batch)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            train_loss_total += loss.item() * X_batch.size(0)

        avg_train_loss = train_loss_total / len(train_loader.dataset)

        model.eval()
        val_loss_total = 0.0

        with torch.no_grad():
            for X_batch, y_batch in val_loader:
                X_batch = X_batch.to(device)
                y_batch = y_batch.to(device)

                logits = model(X_batch)
                loss = loss_fn(logits, y_batch)

                val_loss_total += loss.item() * X_batch.size(0)

        avg_val_loss = val_loss_total / len(val_loader.dataset)

        print(
            f"Epoch {epoch:02d}/{EPOCHS} | "
            f"train_loss={avg_train_loss:.8f} | "
            f"val_loss={avg_val_loss:.8f}"
        )

        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss

            torch.save(
                {
                    "model_state_dict": model.state_dict(),
                    "input_channels": X_train.shape[1],
                    "output_size": y_train.shape[1],
                    "window_size": X_train.shape[2],
                    "feature_columns": FEATURE_COLUMNS,
                    "target_columns": TARGET_COLUMNS,
                    "thresholds": {
                        "return_5m": RETURN_5M_THRESHOLD,
                        "return_10m": RETURN_10M_THRESHOLD,
                        "return_30m": RETURN_30M_THRESHOLD,
                    },
                    "last_train_timestamp": metadata_train[-1]["target_30m_timestamp"],
                    "last_val_timestamp": metadata_val[-1]["target_30m_timestamp"],
                    "best_val_loss": best_val_loss,
                },
                MODEL_OUT,
            )

            print(f"Saved new best model to {MODEL_OUT}")

    print(f"\nTraining complete. Best val loss: {best_val_loss:.8f}")


if __name__ == "__main__":
    main()