import numpy as np
def ohlcv_to_features(ohlcv: np.ndarray) -> np.ndarray:
    """
    Convert raw OHLCV data into model features.

    Input shape:
        (N_TIMESTEPS, 5)

    Input column order:
        [open, high, low, close, volume]

    Output shape:
        (N_TIMESTEPS, 8)

    Output feature order:
        [
            close_ret,
            open_rel,
            high_rel,
            low_rel,
            body,
            upper_wick,
            lower_wick,
            vol_z,
        ]
    """
    if ohlcv.ndim != 2 or ohlcv.shape[1] != 5:
        raise ValueError(f"Expected OHLCV shape (n, 5), got {ohlcv.shape}")
    
    eps = 1e-8

    open_prices = ohlcv[:, 0]
    high_prices = ohlcv[:, 1]
    low_prices = ohlcv[:, 2]
    close_prices = ohlcv[:, 3]
    volumes = ohlcv[:, 4]

    # Percent return of close relative to previous close
    close_ret = np.zeros_like(close_prices)
    close_ret[1:] = (
        close_prices[1:] - close_prices[:-1]
    ) / (close_prices[:-1] + eps)

    # Relative OHLC position compared to close
    open_rel = (open_prices - close_prices) / (close_prices + eps)
    high_rel = (high_prices - close_prices) / (close_prices + eps)
    low_rel = (low_prices - close_prices) / (close_prices + eps)

    # Candle body and wick features
    body = (close_prices - open_prices) / (close_prices + eps)

    upper_wick = (
        high_prices - np.maximum(open_prices, close_prices)
    ) / (close_prices + eps)

    lower_wick = (
        np.minimum(open_prices, close_prices) - low_prices
    ) / (close_prices + eps)

    # Volume z-score within the batch
    volume_mean = np.mean(volumes)
    volume_std = np.std(volumes)

    vol_z = (volumes - volume_mean) / (volume_std + eps)

    features = np.column_stack([
        close_ret,
        open_rel,
        high_rel,
        low_rel,
        body,
        upper_wick,
        lower_wick,
        vol_z,
    ])

    return features.astype(np.float32)