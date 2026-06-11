from pathlib import Path
import sqlite3
import pandas as pd
import yfinance as yf
from datetime import datetime, timezone
import math

TICKERS = [
    "AAPL", "MSFT", "NVDA", "AMD", "GOOGL",
    "AMZN", "META", "TSLA", "JPM", "V",
    "XOM", "UNH", "COST", "SPY", "QQQ"
]

YFINANCE_INTERVAL = "5m"
DB_INTERVAL = "FIVE_MINUTES"
PERIOD = "60d"
DB_PATH = Path("data/ohlcv.db")

INTERVAL_CONFIGS = [
    {
        "yfinance_interval": "1m",
        "db_interval": "ONE_MINUTE",
        "max_period_days": 7,
    },
    {
        "yfinance_interval": "5m",
        "db_interval": "FIVE_MINUTES",
        "max_period_days": 60,
    },
]

def download_ohlcv(
    ticker: str,
    yfinance_interval: str,
    db_interval: str,
    period: str
) -> pd.DataFrame:
    df = yf.download(
        ticker,
        interval=yfinance_interval,
        period=period,
        auto_adjust=False,
        prepost=False,
        progress=False,
    )

    if df.empty:
        return df

    if isinstance(df.columns, pd.MultiIndex):
        df.columns = df.columns.get_level_values(0)

    df = df.reset_index()

    time_col = "Datetime" if "Datetime" in df.columns else "Date"

    df = df.rename(columns={
        time_col: "timestamp",
        "Open": "open",
        "High": "high",
        "Low": "low",
        "Close": "close",
        "Volume": "volume",
    })

    df = df[["timestamp", "open", "high", "low", "close", "volume"]]

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df["timestamp"] = df["timestamp"].astype("int64") // 1_000_000_000

    df["ticker"] = ticker.upper()
    df["interval"] = db_interval

    return df[[
        "ticker",
        "interval",
        "timestamp",
        "open",
        "high",
        "low",
        "close",
        "volume",
    ]]


def upsert_ohlcv(df: pd.DataFrame, db_path: Path) -> int:
    db_path.parent.mkdir(parents=True, exist_ok=True)

    rows = list(df.itertuples(index=False, name=None))

    with sqlite3.connect(db_path) as conn:
        conn.executemany(
            """
            INSERT INTO ohlcv (
                ticker,
                interval,
                timestamp,
                open,
                high,
                low,
                close,
                volume
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(ticker, interval, timestamp)
            DO UPDATE SET
                open = excluded.open,
                high = excluded.high,
                low = excluded.low,
                close = excluded.close,
                volume = excluded.volume;
            """,
            rows,
        )

        conn.commit()

    return len(rows)


def update_ticker(ticker: str, interval_config: dict) -> None:
    yfinance_interval = interval_config["yfinance_interval"]
    db_interval = interval_config["db_interval"]
    max_period_days = interval_config["max_period_days"]

    period = period_for_update(
        ticker=ticker,
        db_interval=db_interval,
        max_period_days=max_period_days,
        db_path=DB_PATH,
    )

    df = download_ohlcv(
        ticker=ticker,
        yfinance_interval=yfinance_interval,
        db_interval=db_interval,
        period=period,
    )

    if df.empty:
        print(f"No data returned for {ticker} {db_interval} using period {period}")
        return

    row_count = upsert_ohlcv(df, DB_PATH)

    print(
        f"Upserted {row_count} rows for {ticker} {db_interval} "
        f"using yfinance period {period}"
    )

def latest_timestamp_for(
    ticker: str,
    db_interval: str,
    db_path: Path
) -> int | None:
    if not db_path.exists():
        return None

    with sqlite3.connect(db_path) as conn:
        cursor = conn.execute(
            """
            SELECT MAX(timestamp)
            FROM ohlcv
            WHERE ticker = ? AND interval = ?;
            """,
            (ticker.upper(), db_interval),
        )

        result = cursor.fetchone()[0]

    return result

def period_for_update(
    ticker: str,
    db_interval: str,
    max_period_days: int,
    db_path: Path
) -> str:
    latest_timestamp = latest_timestamp_for(ticker, db_interval, db_path)

    if latest_timestamp is None:
        return f"{max_period_days}d"

    now_timestamp = int(datetime.now(timezone.utc).timestamp())
    seconds_missing = now_timestamp - latest_timestamp

    if seconds_missing <= 0:
        return "1d"

    days_missing = math.ceil(seconds_missing / 86_400) + 1

    days_to_query = min(days_missing, max_period_days)

    return f"{days_to_query}d"

def main() -> None:
    for interval_config in INTERVAL_CONFIGS:
        for ticker in TICKERS:
            try:
                update_ticker(ticker, interval_config)
            except Exception as e:
                print(f"Failed to update {ticker} {interval_config['db_interval']}: {e}")


if __name__ == "__main__":
    main()