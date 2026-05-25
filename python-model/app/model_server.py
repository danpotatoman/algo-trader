# How to start server:
# 1. be in directory algo-trader
# 2. .\python-model\venv\Scripts\Activate
# 3. cd python-model
# 4. uvicorn app.model_server:app --reload
# http://127.0.0.1:8000/docs#/
# http://localhost:8000

from typing import List
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import numpy as np
import torch
from app.stock_cnn import StockCNN
from app.feature_transformer import ohlcv_to_features
import os

app = FastAPI(title="Stock Prediction Model API")

OHLCV_COLUMNS = ["open", "high", "low", "close", "volume"]
N_OHLCV_FIELDS = 5
N_FIELDS_TRANSFORMED = 8
N_TIMESTEPS = 10


class PredictRequest(BaseModel):
    ticker: str = "AAPL"
    data: List[List[float]] = Field(
        ...,
        description="10 time steps, each containing OHLCV: [open, high, low, close, volume]",
    )


class PredictResponse(BaseModel):
    ticker: str
    prediction: int
    confidence: float
    label: str

model = StockCNN()

BASE_DIR = os.path.dirname(os.path.dirname(__file__))
model_path = os.path.join(BASE_DIR, "saved_models", "stock_cnn.pt")
model.load_state_dict(torch.load(model_path, map_location="cpu"))

model.eval()

@app.get("/")
def root():
    return {"status": "running"}

@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    ohlcv = np.array(request.data, dtype=np.float32)

    print(f"[INPUT] ticker={request.ticker}, raw_shape={ohlcv.shape}")

    if ohlcv.shape != (N_TIMESTEPS, N_OHLCV_FIELDS):
        raise HTTPException(
            status_code=400,
            detail=f"Expected raw OHLCV shape {(N_TIMESTEPS, N_OHLCV_FIELDS)}, got {ohlcv.shape}",
        )

    data = ohlcv_to_features(ohlcv)

    print(f"[FEATURES] ticker={request.ticker}, feature_shape={data.shape}")

    if data.shape != (N_TIMESTEPS, N_FIELDS_TRANSFORMED):
        raise HTTPException(
            status_code=500,
            detail=f"Feature transformation failed. Expected {(N_TIMESTEPS, N_FIELDS_TRANSFORMED)}, got {data.shape}",
        )

    x = torch.tensor(data).unsqueeze(0)
    x = x.permute(0, 2, 1)

    with torch.no_grad():
        output = model(x)

        print("Raw output:", output)
        print("Output shape:", output.shape)

        prob_up = torch.sigmoid(output).item()

    pred = 1 if prob_up >= 0.5 else 0
    conf = prob_up if pred == 1 else 1 - prob_up

    label = "UP" if pred == 1 else "DOWN"

    print(f"[DECISION] ticker={request.ticker}, pred={label}, conf={conf:.4f}")

    return PredictResponse(
        ticker=request.ticker,
        prediction=pred,
        confidence=conf,
        label=label,
    )
