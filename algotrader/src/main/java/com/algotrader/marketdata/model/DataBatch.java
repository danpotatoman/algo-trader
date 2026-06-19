package com.algotrader.marketdata.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an ordered batch of {@link StampedOHLCV} data points for a single
 * ticker and time interval.
 *
 * <p>A {@code DataBatch} enforces the following invariants:
 * <ul>
 *     <li>All rows belong to the same ticker</li>
 *     <li>Rows are strictly ordered by increasing timestamp</li>
 *     <li>No null or invalid entries are allowed</li>
 *     <li>All rows are for the same time interval</li>
 * </ul>
 *
 * <p>This class is mutable and is typically populated by market data providers
 * and sliding-window components before being passed to prediction models.
 *
 * <p>It serves as the primary model-input object within the trading system.
 * Market data is accumulated into a {@code DataBatch} before being supplied to
 * prediction providers.
 */
public class DataBatch {

    /**
     * Internal list of OHLCV rows, maintained in strictly increasing timestamp order.
     */
    private final List<StampedOHLCV> rows = new ArrayList<>();

    /**
     * The TimeInterval of all OHLCV rows.
     */
    private final TimeInterval interval;

    /**
     * Constructs an empty data batch for a specific interval.
     *
     * @param interval interval shared by all rows in the batch
     * @throws IllegalArgumentException if {@code interval} is null
     */
    public DataBatch(TimeInterval interval) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        this.interval = interval;
    }

    /**
     * Constructs a data batch initialized with a single row.
     *
     * @param interval interval shared by all rows in the batch
     * @param row initial row
     * @throws IllegalArgumentException if any argument is invalid
     */
    public DataBatch(TimeInterval interval, StampedOHLCV row) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        this.interval = interval;

        add(row);
    }

    /**
     * Constructs a data batch from a collection of rows.
     *
     * @param interval interval shared by all rows in the batch
     * @param rows rows used to initialize the batch
     * @throws IllegalArgumentException if any argument is invalid
     */
    public DataBatch(TimeInterval interval, List<StampedOHLCV> rows) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        this.interval = interval;

        if (rows == null) {
            throw new IllegalArgumentException("Rows cannot be null.");
        }

        for (StampedOHLCV row : rows) {
            add(row);
        }
    }

    /**
     * Adds a single row to the batch.
     *
     * <p>The row must:
     * <ul>
     *     <li>Not be null</li>
     *     <li>Match the ticker of existing rows (if any)</li>
     *     <li>Have a timestamp strictly greater than the last row</li>
     * </ul>
     *
     * @param row the row to add
     * @throws IllegalArgumentException if the row violates any constraints
     */
    public void add(StampedOHLCV row) {
        validateNewRow(row);
        rows.add(row);
    }

    /**
     * Adds multiple rows to the batch.
     *
     * <p>Each row is validated individually and added in sequence.
     *
     * @param newRows the rows to add
     * @throws IllegalArgumentException if the list is null or any row is invalid
     */
    public void addAll(List<StampedOHLCV> newRows) {
        if (newRows == null) {
            throw new IllegalArgumentException("Rows cannot be null.");
        }

        for (StampedOHLCV row : newRows) {
            add(row);
        }
    }

    /**
     * Validates a new row before adding it to the batch.
     *
     * <p>Ensures:
     * <ul>
     *     <li>The row is non-null</li>
     *     <li>The ticker is consistent</li>
     *     <li>The timestamp is strictly increasing</li>
     *     <li>The OHLCV data is present</li>
     * </ul>
     *
     * @param row the row to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateNewRow(StampedOHLCV row) {
        if (row == null) {
            throw new IllegalArgumentException("StampedOHLCV row cannot be null.");
        }

        if (row.ticker() == null || row.ticker().isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        if (row.candleOpenTime() == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }

        if (row.ohlcv() == null) {
            throw new IllegalArgumentException("OHLCV cannot be null.");
        }

        if (!rows.isEmpty()) {
            StampedOHLCV lastRow = rows.get(rows.size() - 1);

            if (!lastRow.ticker().equals(row.ticker())) {
                throw new IllegalArgumentException(
                        "Ticker mismatch: expected " + lastRow.ticker()
                                + ", got " + row.ticker()
                );
            }

            if (!row.candleOpenTime().isAfter(lastRow.candleOpenTime())) {
                throw new IllegalArgumentException(
                        "Rows must be strictly ordered by timestamp. New row has timestamp "
                                + row.candleOpenTime()
                                + ", but previous timestamp was " + lastRow.candleOpenTime()
                );
            }
        }

        if (row.interval() != interval) {
            throw new IllegalArgumentException("Row's interval must match with this DataBatch's interval.");
        }
    }

    public String getTicker() {
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(0).ticker();
    }

    public int getBatchSize() {
        return rows.size();
    }

    public TimeInterval getInterval() {
        return interval;
    }

    /**
     * Returns the opening timestamp of the final candle in the batch.
     *
     * @return opening timestamp of the last candle, or {@code null} if empty
     */
    public Instant getLastCandleTimestamp() {
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(rows.size() - 1).candleOpenTime();
    }

    /**
     * Returns the closing timestamp of the final candle in the batch.
     *
     * <p>This is calculated by adding the batch interval duration to the final
     * candle's opening timestamp.
     *
     * @return closing timestamp of the last candle, or {@code null} if empty
     */
    public Instant getLastCandleCloseTimestamp() {
        Instant lastCandleTimestamp = getLastCandleTimestamp();

        if (lastCandleTimestamp == null) {
            return null;
        }

        return lastCandleTimestamp.plus(interval.getDuration());
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Returns an unmodifiable view of the rows in this batch.
     *
     * @return an immutable list of {@link StampedOHLCV} rows
     */
    public List<StampedOHLCV> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Converts the batch into a model input matrix.
     *
     * <p>The returned array has shape:
     *
     * <pre>
     * [batchSize][5]
     * </pre>
     *
     * with columns ordered as:
     *
     * <pre>
     * [open, high, low, close, volume]
     * </pre>
     *
     * <p><b>Important:</b> The column ordering is part of the contract between
     * the Java trading system and Python prediction models. Changes to this
     * ordering require corresponding updates to model preprocessing logic.
     *
     * @return feature matrix representation of the batch
     */
    public double[][] toFeatureArray() {
        double[][] array = new double[rows.size()][5];

        for (int i = 0; i < rows.size(); i++) {
            OHLCV ohlcv = rows.get(i).ohlcv();

            array[i][0] = ohlcv.open();
            array[i][1] = ohlcv.high();
            array[i][2] = ohlcv.low();
            array[i][3] = ohlcv.close();
            array[i][4] = ohlcv.volume();
        }

        return array;
    }

    /**
     * Returns a verbose representation of the batch including every row.
     *
     * <p>Intended for debugging and diagnostic output.
     *
     * @return detailed string representation of the batch
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder(toString());
        sb.append("\nRows:\n");

        for (StampedOHLCV row : rows) {
            sb.append(row).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        if (rows.isEmpty()) {
            return "DataBatch{interval=" + interval + ", empty}";
        }

        String ticker = getTicker();
        int size = getBatchSize();
        Instant start = rows.get(0).candleOpenTime();
        Instant end = getLastCandleTimestamp();
        return "DataBatch{" +
                "ticker='" + ticker + '\'' +
                ", interval=" + interval +
                ", size=" + size +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}