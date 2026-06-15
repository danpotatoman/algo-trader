import numpy as np


def chronological_split(
    X: np.ndarray,
    y: np.ndarray,
    metadata: list[dict],
    train_ratio: float = 0.70,
    val_ratio: float = 0.15,
):
    timestamps = np.array(
        [m["window_end_timestamp"] for m in metadata],
        dtype=np.int64,
    )

    sorted_indices = np.argsort(timestamps)

    X = X[sorted_indices]
    y = y[sorted_indices]
    metadata = [metadata[i] for i in sorted_indices]

    n = len(X)

    train_end = int(n * train_ratio)
    val_end = int(n * (train_ratio + val_ratio))

    return (
        X[:train_end],
        y[:train_end],
        metadata[:train_end],
        X[train_end:val_end],
        y[train_end:val_end],
        metadata[train_end:val_end],
        X[val_end:],
        y[val_end:],
        metadata[val_end:],
    )