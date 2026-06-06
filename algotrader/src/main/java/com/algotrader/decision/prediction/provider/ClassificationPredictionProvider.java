package com.algotrader.decision.prediction.provider;

import com.algotrader.decision.dataobjects.ClassificationPrediction;

/**
 * Specialization of {@link PredictionProvider} that produces
 * {@link ClassificationPrediction} instances.
 *
 * <p>This interface exists to represent providers backed by classification
 * models, where predictions are expressed as discrete classes rather than
 * continuous-valued forecasts.
 *
 * <p>Classification models were used extensively during early development
 * of the trading system and remain supported for experimentation and
 * backwards compatibility.
 *
 * <p>Most consumers should depend on the more general
 * {@link PredictionProvider} abstraction unless classification-specific
 * behavior is required.
 */
public interface ClassificationPredictionProvider
        extends PredictionProvider<ClassificationPrediction> {
}