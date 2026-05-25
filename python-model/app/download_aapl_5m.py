import yfinance as yf
from pathlib import Path

symbol = "AAPL"
interval = "5m"
period = "5d" 

output_dir = Path("data")
output_dir.mkdir(exist_ok=True)

output_file = output_dir / "aapl_5m_ohlcv.csv"

df = yf.download(
    tickers=symbol,
    period=period,
    interval=interval,
    auto_adjust=False,
    progress=True
)

# Clean up the dataframe
df = df.reset_index()

# If yfinance returns multi-level columns, flatten them
if isinstance(df.columns[0], tuple):
    df.columns = [col[0] for col in df.columns]

# Keep only the columns we want
df = df[["Datetime", "Open", "High", "Low", "Close", "Volume"]]

# Rename for Java-friendly parsing
df.columns = ["timestamp", "open", "high", "low", "close", "volume"]

df.to_csv(output_file, index=False)

print(f"Saved {len(df)} rows to {output_file}")
print(df.head())