"""Decode the volatility training target at the serving boundary."""
import math

VOLATILITY_DESCRIPTION = (
    "Population standard deviation (ddof=0) of six consecutive five-minute simple "
    "returns over the forward 30-minute target window, expressed as a return "
    "fraction, not annualized; inverse-scaled using checkpoint target_scale."
)

VOLATILITY_CONTRACT = {
    "version": "volatility-return-fraction-v1",
    "units": "return_fraction",
    "statistic": "population_standard_deviation",
    "ddof": 0,
    "returnType": "simple",
    "returnIntervalMinutes": 5,
    "returnCount": 6,
    "horizonMinutes": 30,
    "annualized": False,
    "inverseTargetScalingApplied": True,
    "scaleMetadata": "target_scale",
    "outputStatisticsUnits": "return_fraction",
}


def require_target_scale(checkpoint):
    scale = checkpoint.get("target_scale")
    if (isinstance(scale, bool) or not isinstance(scale, (int, float))
            or not math.isfinite(scale) or scale <= 0):
        raise ValueError("Volatility checkpoint target_scale must exist and be finite and positive")
    return float(scale)


def predict_return_fraction_volatility(model, inputs):
    """Shared single/batch postprocessing; model weights and forward stay unchanged."""
    scale = require_target_scale({"target_scale": getattr(model, "target_scale", None)})
    return model(inputs) / scale
