import sqlite3
from pathlib import Path

import pandas as pd


def load_five_minute_dataframe(db_path: str | Path) -> pd.DataFrame:
    """
    Loads all FIVE_MINUTES OHLCV rows from SQLite into a pandas DataFrame.

    Columns:
        ticker
        interval
        timestamp
        open
        high
        low
        close
        volume
    """
    db_path = Path(db_path)

    if not db_path.exists():
        raise FileNotFoundError(f"Database not found at: {db_path}")

    query = """
        SELECT ticker, interval, timestamp, open, high, low, close, volume
        FROM ohlcv
        WHERE interval = ?
        ORDER BY ticker, timestamp
    """

    with sqlite3.connect(db_path) as conn:
        df = pd.read_sql_query(
            query,
            conn,
            params=("FIVE_MINUTES",)
        )

    df["timestamp"] = df["timestamp"].astype("int64")
    df["datetime"] = pd.to_datetime(df["timestamp"], unit="s", utc=True)

    return df


def print_summary(df: pd.DataFrame) -> None:
    print(f"Loaded {len(df):,} total FIVE_MINUTES rows")
    print(f"Loaded {df['ticker'].nunique()} tickers")

    summary = (
        df.groupby("ticker")
        .agg(
            rows=("ticker", "size"),
            first_timestamp=("timestamp", "min"),
            last_timestamp=("timestamp", "max"),
        )
        .sort_index()
    )

    print()
    print(summary)


if __name__ == "__main__":
    project_root = Path(__file__).resolve().parents[2]
    db_path = project_root / "data" / "ohlcv.db"

    df = load_five_minute_dataframe(db_path)
    print_summary(df)