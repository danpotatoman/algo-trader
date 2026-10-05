# AlgoTrader

AlgoTrader is a Java-based algorithmic trading framework that uses
machine learning models hosted in Python to generate trading decisions.

The project supports:

- Historical backtesting
- Config-driven trading sessions
- SQLite-backed OHLCV storage
- JSON trade logging
- Modular trading strategies
- Classification models are fully supported.
- Regression infrastructure exists but regression-based trade generation strategies are not yet implemented.

## Features

- Sliding-window market data pipeline
- Python model integration via HTTP
- Historical paper trading
- Configurable trading sessions
- SQLite market data storage
- Structured JSON cycle logging
- Extensible strategy architecture

## Quick Start

For an analysis-ready multi-ticker run with equity valuations, immutable data,
model provenance and timing, see [Showcase backtest](docs/showcase-backtest.md).

### Start the model server

cd python-model
uvicorn app.model_server:app

### Run the Java application

(i still need to set up maven)

## Configuration

Trading sessions are defined in:

config/session/

Models are defined in:

config/model/

Trade generators are defined in:

config/trade-generator/

For detailed configuration information see:

docs/configuration.md (i need to add this still)

## Architecture

Classification-with-volatility endpoints require 31 completed candles per ticker:
one preceding candle for return context followed by the 30 model candles.
Python uses the context close to calculate the first model candle's return,
then discards the context feature row. Prices are normalized against the final
model candle's close, matching training. Model tensors remain 30 candles long
and the forecast horizon remains 30 minutes. Restart the Python server when
using the updated endpoint configs; older 30-row requests are rejected.

The multi-ticker backtest treats each cycle timestamp as the decision and
execution time. Prediction windows contain only candles completed by that
time; OHLCV timestamps identify candle opens. At 10:05, a five-minute window
ends with the 10:00 candle, entry is simulated at the 10:05 open, and a
30-minute holding period ends at 10:35. Session configuration keys
`firstCandleTimestamp` and `lastCandleTimestamp` bound these cycle times.
Earlier candles are loaded to provide the first decision's input history.
Fills at the cycle's opening price assume zero inference latency and no slippage.

See:

docs/architecture.md (i need to add this still)

## Current Status

Implemented:
- SQLite OHLCV storage
- Historical backtesting
- Classification pipeline
- Regression pipeline foundation
- JSON trade logging

Planned:
- Regression model support
- Live trading
- Multi-session scheduling

## Technologies

Java 17
Maven*
SQLite
Python
FastAPI
PyTorch
Jackson

## Disclaimer

This project is for educational and research purposes only.
It is not financial advice.
