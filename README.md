# AlgoTrader

AlgoTrader is a Java-based algorithmic trading framework that uses
machine learning models hosted in Python to generate trading decisions.

The project supports:

- Historical backtesting
- Config-driven trading sessions
- Classification and regression models
- SQLite-backed OHLCV storage
- JSON trade logging
- Modular trading strategies

## Features

- Sliding-window market data pipeline
- Python model integration via HTTP
- Historical paper trading
- Configurable trading sessions
- SQLite market data storage
- Structured JSON cycle logging
- Extensible strategy architecture

## Quick Start

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