# How to start server:
# 1. be in directory algo-trader
# 2. .\python-model\venv\Scripts\Activate
# 3. cd python-model
# 4. uvicorn app.model_server:app --reload
# http://127.0.0.1:8000/docs#/
# http://localhost:8000

from typing import List
import os
import numpy as np
import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from app.stock_cnn import StockCNN
from app.cnn_v1_feature_transformer import ohlcv_to_features
from training.cnn_threshold_classification.model import CNNThresholdClassificationModel
from training.cnn_volatility_regression.model import CNNVolatilityRegressionModel
from app.cnn_classification_volatility_v1_feature_transformer import rows_to_features

app = FastAPI(title="Stock Prediction Model API")


N_OHLCV_FIELDS = 5

BASE_DIR = os.path.dirname(os.path.dirname(__file__))
SAVED_MODELS_DIR = os.path.join(BASE_DIR, "saved_models")


class PredictRequest(BaseModel):
    ticker: str = "AAPL"
    data: List[List[float]] = Field(
        ...,
        description="Time steps, each containing OHLCV: [open, high, low, close, volume]",
    )

class PredictResponse(BaseModel):
    ticker: str
    prediction: int
    confidence: float
    horizonMinutes: int
    label: str

class PredictionRow(BaseModel):
    timestamp: str
    open: float
    high: float
    low: float
    close: float
    volume: float

class ClassificationVolatilityPredictRequest(BaseModel):
    ticker: str = "AAPL"
    rows: List[PredictionRow]

class ClassificationWithVolatilityResponse(BaseModel):
    ticker: str
    probability: float
    volatility: float
    horizonMinutes: int

class ClassificationVolatilityBatchPredictRequest(BaseModel):
    batches: List[ClassificationVolatilityPredictRequest]


class ClassificationWithVolatilityPrediction(BaseModel):
    ticker: str
    probability: float
    volatility: float
    horizonMinutes: int


class ClassificationWithVolatilityBatchResponse(BaseModel):
    predictions: List[ClassificationWithVolatilityPrediction]

class CnnV1Config:
    model_id = "cnn-v1"
    n_timesteps = 10
    n_raw_fields = N_OHLCV_FIELDS
    n_transformed_fields = 8
    horizon_minutes = 10

class CnnClassificationVolatilityV1Config:
    classification_model_id = "cnn-threshold-classification-v1"
    volatility_model_id = "cnn-volatility-v1"
    n_timesteps = 30
    n_features = 10
    horizon_minutes = 30

def load_stock_cnn(model_id: str) -> StockCNN:
    model_path = os.path.join(SAVED_MODELS_DIR, f"{model_id}.pt")

    if not os.path.exists(model_path):
        raise RuntimeError(f"Model file not found: {model_path}")

    model = StockCNN()
    model.load_state_dict(torch.load(model_path, map_location="cpu"))
    model.eval()

    return model


def validate_shape(
        data: np.ndarray,
        expected_shape: tuple[int, int],
        shape_name: str,
        status_code: int,
) -> None:
    if data.shape != expected_shape:
        raise HTTPException(
            status_code=status_code,
            detail=f"Expected {shape_name} shape {expected_shape}, got {data.shape}",
        )

def batch_rows_to_cnn_tensor(
        batches: List[ClassificationVolatilityPredictRequest],
) -> torch.Tensor:
    tensors = [rows_to_cnn_tensor(batch.rows) for batch in batches]

    # each tensor is [1, features, timesteps]
    # result is [batch, features, timesteps]
    return torch.cat(tensors, dim=0)

def cnn_v1_ohlcv_to_tensor(ohlcv: np.ndarray) -> torch.Tensor:
    validate_shape(
        data=ohlcv,
        expected_shape=(CnnV1Config.n_timesteps, CnnV1Config.n_raw_fields),
        shape_name="cnn-v1 raw OHLCV",
        status_code=400,
    )

    features = ohlcv_to_features(ohlcv)

    validate_shape(
        data=features,
        expected_shape=(CnnV1Config.n_timesteps, CnnV1Config.n_transformed_fields),
        shape_name="cnn-v1 transformed feature",
        status_code=500,
    )

    x = torch.tensor(features, dtype=torch.float32).unsqueeze(0)

    # [batch, timesteps, features] -> [batch, features, timesteps]
    return x.permute(0, 2, 1)


def predict_cnn_v1_probability(model: StockCNN, ohlcv: np.ndarray) -> float:
    x = cnn_v1_ohlcv_to_tensor(ohlcv)

    with torch.no_grad():
        output = model(x)
        probability = torch.sigmoid(output).item()

    return probability

def load_threshold_classification_model(model_id: str) -> CNNThresholdClassificationModel:
    model_path = os.path.join(SAVED_MODELS_DIR, f"{model_id}.pt")

    if not os.path.exists(model_path):
        raise RuntimeError(f"Model file not found: {model_path}")

    checkpoint = torch.load(model_path, map_location="cpu")

    model = CNNThresholdClassificationModel()
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()

    return model


def load_volatility_regression_model(model_id: str) -> CNNVolatilityRegressionModel:
    model_path = os.path.join(SAVED_MODELS_DIR, f"{model_id}.pt")

    if not os.path.exists(model_path):
        raise RuntimeError(f"Model file not found: {model_path}")

    checkpoint = torch.load(model_path, map_location="cpu")

    model = CNNVolatilityRegressionModel()
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()

    return model

def rows_to_cnn_tensor(rows: List[PredictionRow]) -> torch.Tensor:
    if len(rows) != CnnClassificationVolatilityV1Config.n_timesteps:
        raise HTTPException(
            status_code=400,
            detail=(
                f"Expected {CnnClassificationVolatilityV1Config.n_timesteps} rows, "
                f"got {len(rows)}"
            ),
        )

    row_dicts = [row.model_dump() for row in rows]
    features = rows_to_features(row_dicts)

    expected_shape = (
        CnnClassificationVolatilityV1Config.n_timesteps,
        CnnClassificationVolatilityV1Config.n_features,
    )

    if features.shape != expected_shape:
        raise HTTPException(
            status_code=500,
            detail=f"Expected feature shape {expected_shape}, got {features.shape}",
        )

    x = torch.tensor(features, dtype=torch.float32).unsqueeze(0)

    # [batch, timesteps, features] -> [batch, features, timesteps]
    return x.permute(0, 2, 1)

cnn_v1_model = load_stock_cnn(CnnV1Config.model_id)

classification_volatility_v1_classification_model = load_threshold_classification_model(
    CnnClassificationVolatilityV1Config.classification_model_id
)

classification_volatility_v1_volatility_model = load_volatility_regression_model(
    CnnClassificationVolatilityV1Config.volatility_model_id
)


@app.get("/")
def root():
    return {"status": "running"}


@app.post("/predict/cnn-v1", response_model=PredictResponse)
def predict_cnn_v1(request: PredictRequest):
    ohlcv = np.array(request.data, dtype=np.float32)

    print(f"[INPUT] endpoint=/predict/cnn-v1, ticker={request.ticker}, raw_shape={ohlcv.shape}")

    prob_up = predict_cnn_v1_probability(cnn_v1_model, ohlcv)

    pred = 1 if prob_up >= 0.5 else 0
    conf = prob_up if pred == 1 else 1 - prob_up
    label = "UP" if pred == 1 else "DOWN"

    print(
        f"[DECISION] endpoint=/predict/cnn-v1, "
        f"ticker={request.ticker}, pred={label}, conf={conf:.4f}"
    )

    return PredictResponse(
        ticker=request.ticker,
        prediction=pred,
        confidence=conf,
        horizonMinutes=CnnV1Config.horizon_minutes,
        label=label,
    )

@app.post(
    "/predict/cnn-classification-volatility-v1",
    response_model=ClassificationWithVolatilityResponse,
)
def predict_cnn_classification_volatility_v1(
        request: ClassificationVolatilityPredictRequest,
):
    x = rows_to_cnn_tensor(request.rows)

    with torch.no_grad():
        classification_logits = classification_volatility_v1_classification_model(x)
        probability = torch.sigmoid(classification_logits).item()

        volatility_output = classification_volatility_v1_volatility_model(x)
        volatility = volatility_output.item()

    return ClassificationWithVolatilityResponse(
        ticker = request.ticker,
        probability=probability,
        volatility=volatility,
        horizonMinutes=CnnClassificationVolatilityV1Config.horizon_minutes,
    )

@app.post(
    "/predict/cnn-classification-volatility-v1/batch",
    response_model=ClassificationWithVolatilityBatchResponse,
)
def predict_cnn_classification_volatility_v1_batch(
        request: ClassificationVolatilityBatchPredictRequest,
):
    if len(request.batches) == 0:
        return ClassificationWithVolatilityBatchResponse(predictions=[])

    x = batch_rows_to_cnn_tensor(request.batches)

    with torch.no_grad():
        classification_logits = classification_volatility_v1_classification_model(x)
        probabilities = torch.sigmoid(classification_logits).squeeze(-1)

        volatility_outputs = classification_volatility_v1_volatility_model(x)
        volatilities = volatility_outputs.squeeze(-1)

    predictions = []

    for batch, probability, volatility in zip(
            request.batches,
            probabilities.tolist(),
            volatilities.tolist(),
    ):
        predictions.append(
            ClassificationWithVolatilityPrediction(
                ticker=batch.ticker,
                probability=probability,
                volatility=volatility,
                horizonMinutes=CnnClassificationVolatilityV1Config.horizon_minutes,
            )
        )

    return ClassificationWithVolatilityBatchResponse(predictions=predictions)