from pathlib import Path
import pandas as pd
import yfinance as yf

TICKERS = [
    "AAPL", "MSFT", "NVDA", "AMD", "GOOGL",
    "AMZN", "META", "TSLA", "JPM", "V",
    "XOM", "UNH", "COST", "SPY", "QQQ"
]
INTERVAL = "1m"
PERIOD = "7d"
DATA_DIR = Path("data/ohlcv")

def filename_for(ticker: str, interval: str) -> Path:
    return DATA_DIR / f"{ticker.lower()}_{interval}_ohlcv.csv"

def download_ohlcv(ticker: str, interval: str, period: str) -> pd.DataFrame:
    df = yf.download(
        ticker,
        interval=interval,
        period=period,
        auto_adjust=False,
        prepost=False,
        progress=False,
    )

    if df.empty:
        return df

    # Flatten possible multi-index columns from yfinance
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = df.columns.get_level_values(0)

    df = df.reset_index()

    # yfinance may call the timestamp column "Datetime" or "Date"
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
    df["ticker"] = ticker.upper()
    df["interval"] = interval

    return df

def merge_with_existing(new_df: pd.DataFrame, csv_path: Path) -> pd.DataFrame:
    if csv_path.exists():
        old_df = pd.read_csv(csv_path, parse_dates=["timestamp"])
        combined = pd.concat([old_df, new_df], ignore_index=True)
    else:
        combined = new_df

    combined["timestamp"] = pd.to_datetime(combined["timestamp"], utc=True)

    combined = (
        combined
        .drop_duplicates(subset=["timestamp", "ticker", "interval"], keep="last")
        .sort_values("timestamp")
        .reset_index(drop=True)
    )

    return combined

def update_ticker(ticker: str, interval: str, period: str) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    csv_path = filename_for(ticker, interval)
    new_df = download_ohlcv(ticker, interval, period)

    if new_df.empty:
        print(f"No data returned for {ticker} {interval}")
        return

    merged = merge_with_existing(new_df, csv_path)
    merged.to_csv(csv_path, index=False)

    print(f"Saved {len(merged)} rows to {csv_path}")

def main():
    for ticker in TICKERS:
        try:
            update_ticker(ticker, INTERVAL, PERIOD)
        except Exception as e:
            print(f"Failed to update {ticker}: {e}")

if __name__ == "__main__":
    main()