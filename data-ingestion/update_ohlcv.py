from pathlib import Path
import sqlite3
import pandas as pd
import yfinance as yf
from datetime import datetime, timezone
import math
import argparse
import json
import uuid
from contextlib import closing

MIN_MARKET_TIMESTAMP = 946684800  # 2000-01-01 UTC; this downloader ingests contemporary intraday data.
INTERVAL_SECONDS = {"ONE_MINUTE": 60, "FIVE_MINUTES": 300}


def unix_seconds(values: pd.Series) -> pd.Series:
    """Normalize explicitly to seconds; pandas may store datetimes in s/ms/us/ns."""
    dates = pd.to_datetime(values, utc=True, errors="raise")
    if dates.isna().any():
        raise ValueError("OHLCV timestamps cannot be missing")
    return dates.dt.as_unit("s").astype("int64")


def validate_timestamps(df: pd.DataFrame) -> None:
    if df.empty:
        return
    stamps = pd.to_numeric(df["timestamp"], errors="raise")
    now = int(datetime.now(timezone.utc).timestamp())
    if (stamps.isna().any() or (stamps % 1 != 0).any()
            or (stamps < MIN_MARKET_TIMESTAMP).any() or (stamps > now).any()):
        raise ValueError("OHLCV timestamps must be contemporary Unix seconds (2000-01-01 through now)")
    for interval, rows in df.groupby("interval"):
        step = INTERVAL_SECONDS.get(interval)
        if step is None or (rows["timestamp"] % step != 0).any():
            raise ValueError(f"OHLCV timestamps are not aligned to {interval} candle opens")
    if df.duplicated(["ticker", "interval", "timestamp"]).any():
        raise ValueError("Duplicate OHLCV candle timestamps in download; refusing a lossy upsert")


def backup_database(db_path: Path) -> Path | None:
    if not db_path.exists():
        return None
    directory = db_path.parent / "backups"
    directory.mkdir(parents=True, exist_ok=True)
    name = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:8]
    destination = directory / f"ohlcv-{name}.db"
    with closing(sqlite3.connect(db_path.resolve().as_uri() + "?mode=ro", uri=True)) as source:
        with closing(sqlite3.connect(destination)) as target:
            source.backup(target)
    return destination

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

    df["timestamp"] = unix_seconds(df["timestamp"])

    df["ticker"] = ticker.upper()
    df["interval"] = db_interval
    validate_timestamps(df)
    # The currently forming candle is not historical input yet.
    now = int(datetime.now(timezone.utc).timestamp())
    df = df.loc[df["timestamp"] + INTERVAL_SECONDS[db_interval] <= now].copy()

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
    validate_timestamps(df)
    if df.empty:
        return 0
    db_path.parent.mkdir(parents=True, exist_ok=True)

    rows = list(df.itertuples(index=False, name=None))

    with closing(sqlite3.connect(db_path)) as conn, conn:
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


def update_ticker(ticker: str, interval_config: dict, full_history: bool = False) -> None:
    yfinance_interval = interval_config["yfinance_interval"]
    db_interval = interval_config["db_interval"]
    max_period_days = interval_config["max_period_days"]

    period = f"{max_period_days}d" if full_history else period_for_update(
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
        raise RuntimeError(f"No completed data returned for {ticker} {db_interval} using period {period}")

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

    with closing(sqlite3.connect(db_path)) as conn:
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
    parser = argparse.ArgumentParser(description="Refresh OHLCV without rebuilding existing history")
    parser.add_argument("--interval", choices=["all", "1m", "5m"], default="all")
    parser.add_argument("--full-history", action="store_true", help="Request the interval's full retention window")
    parser.add_argument("--session", type=Path, help="Read the ticker universe from a session JSON file")
    parser.add_argument("--backup", action="store_true", help="Take a consistent SQLite backup before updates")
    args = parser.parse_args()
    tickers = TICKERS if args.session is None else json.loads(args.session.read_text())["tickers"]
    if args.backup:
        print(f"Database backup: {backup_database(DB_PATH)}", flush=True)
    failures = []
    for interval_config in INTERVAL_CONFIGS:
        if args.interval != "all" and interval_config["yfinance_interval"] != args.interval:
            continue
        for ticker in tickers:
            try:
                update_ticker(ticker, interval_config, args.full_history)
            except Exception as e:
                print(f"Failed to update {ticker} {interval_config['db_interval']}: {e}")
                failures.append(ticker)
    if failures:
        raise SystemExit(f"Refresh incomplete: {len(failures)} ticker/interval requests failed: {failures}")


if __name__ == "__main__":
    main()
