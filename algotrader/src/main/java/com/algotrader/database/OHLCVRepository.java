package com.algotrader.database;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import com.algotrader.data.dataobjects.StampedOHLCV;
import com.algotrader.data.TimeInterval;

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