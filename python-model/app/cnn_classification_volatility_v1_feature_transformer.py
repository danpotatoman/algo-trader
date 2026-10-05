from datetime import datetime, time
from typing import Any

from zoneinfo import ZoneInfo

import numpy as np


OHLCV_COLUMNS = ["open", "high", "low", "close", "volume"]

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

N_OHLCV_FIELDS = len(OHLCV_COLUMNS)
N_FEATURE_FIELDS = len(FEATURE_COLUMNS)

MARKET_TIMEZONE = ZoneInfo("America/New_York")
MARKET_OPEN_TIME = time(hour=9, minute=30)
REGULAR_SESSION_MINUTES = 390.0

def rows_to_features(rows: list[dict[str, Any]]) -> np.ndarray:
    """
    Converts request rows into model features, excluding the first context row.
    The context close supplies the first model candle's preceding close.

    Expected row format:
    {
        "timestamp": "2026-05-18T15:40:00Z",
        "open": 100.0,
        "high": 101.0,
        "low": 99.0,
        "close": 100.5,
        "volume": 1200000
    }

    Returns:
        np.ndarray with shape [len(rows) - 1, 10]
    """
    if len(rows) < 2:
        raise ValueError("rows must contain a context candle and at least one model candle")

    ohlcv = rows_to_ohlcv_array(rows)
    minutes_since_open = rows_to_minutes_since_open(rows)

    return ohlcv_to_features(ohlcv, minutes_since_open)[1:]


def rows_to_ohlcv_array(rows: list[dict[str, Any]]) -> np.ndarray:
    data = [
        [
            row["open"],
            row["high"],
            row["low"],
            row["close"],
            row["volume"],
        ]
        for row in rows
    ]

    return np.asarray(data, dtype=np.float64)


def rows_to_minutes_since_open(rows: list[dict[str, Any]]) -> np.ndarray:
    return np.asarray(
        [
            timestamp_to_minutes_since_open(row["timestamp"])
            for row in rows
        ],
        dtype=np.float64,
    )


def timestamp_to_minutes_since_open(timestamp: str) -> float:
    dt = parse_iso_timestamp(timestamp)
    market_dt = dt.astimezone(MARKET_TIMEZONE)

    market_open_dt = market_dt.replace(
        hour=MARKET_OPEN_TIME.hour,
        minute=MARKET_OPEN_TIME.minute,
        second=0,
        microsecond=0,
    )

    delta = market_dt - market_open_dt

    return delta.total_seconds() / 60.0


def parse_iso_timestamp(timestamp: str) -> datetime:
    """
    Parses ISO timestamps like:
        2026-05-18T15:40:00Z
        2026-05-18T15:40:00+00:00
    """
    normalized = timestamp.replace("Z", "+00:00")
    dt = datetime.fromisoformat(normalized)

    if dt.tzinfo is None:
        raise ValueError(
            f"timestamp must include timezone information: {timestamp}"
        )

    return dt


def ohlcv_to_features(
        ohlcv: np.ndarray,
        minutes_since_open: np.ndarray,
) -> np.ndarray:
    ohlcv = np.asarray(ohlcv, dtype=np.float64)
    minutes_since_open = np.asarray(minutes_since_open, dtype=np.float64)

    validate_ohlcv_shape(ohlcv)
    validate_minutes_since_open_shape(minutes_since_open, ohlcv.shape[0])

    open_ = ohlcv[:, 0]
    high = ohlcv[:, 1]
    low = ohlcv[:, 2]
    close = ohlcv[:, 3]
    volume = ohlcv[:, 4]

    base_close = close[-1]

    open_rel = open_ / base_close - 1.0
    high_rel = high / base_close - 1.0
    low_rel = low / base_close - 1.0
    close_rel = close / base_close - 1.0

    log_volume = np.log1p(volume)

    close_return = np.zeros_like(close)
    close_return[1:] = close[1:] / close[:-1] - 1.0

    range_pct = (high - low) / close
    body_pct = (close - open_) / open_

    time_angle = (
        2.0
        * np.pi
        * minutes_since_open
        / REGULAR_SESSION_MINUTES
    )

    sin_time = np.sin(time_angle)
    cos_time = np.cos(time_angle)

    features = np.column_stack([
        open_rel,
        high_rel,
        low_rel,
        close_rel,
        log_volume,
        close_return,
        range_pct,
        body_pct,
        sin_time,
        cos_time,
    ])

    return features.astype(np.float32)


def validate_ohlcv_shape(ohlcv: np.ndarray) -> None:
    if ohlcv.ndim != 2 or ohlcv.shape[1] != N_OHLCV_FIELDS:
        raise ValueError(
            f"Expected OHLCV shape [timesteps, {N_OHLCV_FIELDS}], "
            f"got {ohlcv.shape}"
        )


def validate_minutes_since_open_shape(
        minutes_since_open: np.ndarray,
        n_timesteps: int,
) -> None:
    expected_shape = (n_timesteps,)

    if minutes_since_open.shape != expected_shape:
        raise ValueError(
            f"Expected minutes_since_open shape {expected_shape}, "
            f"got {minutes_since_open.shape}"
        )
