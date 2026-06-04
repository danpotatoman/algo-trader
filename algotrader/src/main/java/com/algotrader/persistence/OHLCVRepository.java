package com.algotrader.persistence;

import java.util.List;
import java.util.Optional;

import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;

import java.time.Instant;

public interface OHLCVRepository {

        void save(StampedOHLCV candle);

        void saveAll(List<StampedOHLCV> candles);

        Optional<StampedOHLCV> findByKey(
                String ticker,
                TimeInterval interval,
                Instant timestamp
        );

        List<StampedOHLCV> findRange(
                String ticker,
                TimeInterval interval,
                Instant start,
                Instant end
        );

        List<StampedOHLCV> findAll(
                String ticker,
                TimeInterval interval
        );

        Optional<StampedOHLCV> findLatest(
                String ticker,
                TimeInterval interval
        );

        Optional<Instant> findNextTimestamp(
                String ticker,
                TimeInterval interval,
                Instant timestamp
        );
}