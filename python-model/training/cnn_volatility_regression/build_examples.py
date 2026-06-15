import numpy as np
import pandas as pd

from training.window_utils import is_continuous_window

FEATURE_COLUMNS = [
    "open_rel",
    "high_rel",
    "low_rel",
    "close_rel",
    "log_volume",
    "close_return",
    "range_pct",
    "body_pct",
    "sin_time",
    "cos_time",
]

TARGET_COLUMNS = [
    "volatility_30m",
]


def add_base_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df = df.sort_values(["ticker", "timestamp"]).reset_index(drop=True)

    df["log_volume"] = np.log1p(df["volume"])

    df["close_return"] = (
        df.groupby("ticker")["close"]
        .pct_change()
    )

    df["range_pct"] = (
        (df["high"] - df["low"]) / df["close"]
    )

    df["body_pct"] = (
        (df["close"] - df["open"]) / df["open"]
    )

    eastern = df["datetime"].dt.tz_convert("America/New_York")

    market_open_minutes = 9 * 60 + 30
    regular_session_minutes = 390

    minutes_since_midnight = eastern.dt.hour * 60 + eastern.dt.minute
    minutes_since_open = minutes_since_midnight - market_open_minutes

    fraction_of_session = minutes_since_open / regular_session_minutes

    df["sin_time"] = np.sin(2 * np.pi * fraction_of_session)
    df["cos_time"] = np.cos(2 * np.pi * fraction_of_session)

    return df

def build_single_example(
    ticker_df: pd.DataFrame,
    end_idx: int,
    window_size: int,
    horizon_candles: int = 6,
) -> tuple[np.ndarray, np.ndarray, dict] | None:
    """
    Builds one volatility regression training example.

    Returns:
        X: shape (num_features, window_size)
        y: shape (1,)
        metadata: dict
    """
    start_idx = end_idx - window_size + 1
    future_end_idx = end_idx + horizon_candles

    if start_idx < 0:
        return None

    if future_end_idx >= len(ticker_df):
        return None

    window = ticker_df.iloc[start_idx:end_idx + 1].copy()
    future = ticker_df.iloc[end_idx:future_end_idx + 1].copy()

    window_timestamps = window["timestamp"].to_numpy()
    future_timestamps = future["timestamp"].to_numpy()

    if not is_continuous_window(window_timestamps):
        return None

    if not is_continuous_window(future_timestamps):
        return None

    reference_close = float(window["close"].iloc[-1])

    if reference_close <= 0:
        return None

    window["open_rel"] = window["open"] / reference_close - 1.0
    window["high_rel"] = window["high"] / reference_close - 1.0
    window["low_rel"] = window["low"] / reference_close - 1.0
    window["close_rel"] = window["close"] / reference_close - 1.0

    if window[FEATURE_COLUMNS].isna().any().any():
        return None

    future_closes = future["close"].to_numpy(dtype=np.float64)

    if np.any(future_closes <= 0):
        return None

    future_returns = future_closes[1:] / future_closes[:-1] - 1.0
    volatility_30m = float(np.std(future_returns))

    y = np.array(
        [volatility_30m],
        dtype=np.float32,
    )

    if np.isnan(y).any():
        return None

    X = window[FEATURE_COLUMNS].to_numpy(dtype=np.float32).T

    metadata = {
        "ticker": ticker_df["ticker"].iloc[end_idx],
        "window_start_timestamp": int(ticker_df["timestamp"].iloc[start_idx]),
        "window_end_timestamp": int(ticker_df["timestamp"].iloc[end_idx]),
        "target_30m_timestamp": int(ticker_df["timestamp"].iloc[end_idx + horizon_candles]),
        "window_end_datetime": ticker_df["datetime"].iloc[end_idx],
    }

    return X, y, metadata


def build_regression_examples(
    df: pd.DataFrame,
    window_size: int = 30,
    horizon_candles: int = 6,
) -> tuple[np.ndarray, np.ndarray, list[dict]]:
    """
    Builds all valid volatility regression examples.

    Returns:
        X: shape (num_examples, num_features, window_size)
        y: shape (num_examples, 1)
        metadata: one dict per example
    """
    df = df.copy()
    df = df.sort_values(["ticker", "timestamp"]).reset_index(drop=True)

    X_examples = []
    y_examples = []
    metadata = []

    for ticker, ticker_df in df.groupby("ticker", sort=True):
        ticker_df = ticker_df.reset_index(drop=True)

        for end_idx in range(window_size - 1, len(ticker_df) - horizon_candles):
            result = build_single_example(
                ticker_df=ticker_df,
                end_idx=end_idx,
                window_size=window_size,
                horizon_candles=horizon_candles,
            )

            if result is None:
                continue

            X, y, sample_metadata = result

            X_examples.append(X)
            y_examples.append(y)
            metadata.append(sample_metadata)

    if not X_examples:
        raise RuntimeError("No valid regression examples were built.")

    X = np.stack(X_examples).astype(np.float32)
    y = np.stack(y_examples).astype(np.float32)

    return X, y, metadata