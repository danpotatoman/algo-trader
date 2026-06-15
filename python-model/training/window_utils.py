import numpy as np


def is_continuous_window(
    timestamps,
    expected_step_seconds: int = 300,
) -> bool:
    if len(timestamps) < 2:
        return True

    deltas = np.diff(timestamps)

    return np.all(deltas == expected_step_seconds)