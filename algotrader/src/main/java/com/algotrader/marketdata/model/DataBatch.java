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
 * <p>This class is mutable and allows rows to be added incrementally while
 * maintaining consistency constraints.
 *
 * <p>It is typically used as the input to a machine learning model, where the
 * OHLCV data can be accessed either as raw objects or converted into a
 * numerical array via {@link #toFeatureArray()}.
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
     * Constructs an empty {@code DataBatch}.
     */
    public DataBatch(TimeInterval interval) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        this.interval = interval;
    }

    /**
     * Constructs a {@code DataBatch} initialized with a single row.
     *
     * @param row the initial {@link StampedOHLCV} entry
     * @throws IllegalArgumentException if the row is invalid
     */
    public DataBatch(TimeInterval interval, StampedOHLCV row) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null.");
        }

        this.interval = interval;

        add(row);
    }

    /**
     * Constructs a {@code DataBatch} from a list of rows.
     *
     * <p>Rows are added sequentially and validated to ensure they meet ordering
     * and consistency requirements.
     *
     * @param rows the list of rows to initialize the batch with
     * @throws IllegalArgumentException if the list is null or contains invalid data
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

        if (row.timestamp() == null) {
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

            if (!row.timestamp().isAfter(lastRow.timestamp())) {
                throw new IllegalArgumentException(
                        "Rows must be strictly ordered by timestamp. New row has timestamp "
                                + row.timestamp()
                                + ", but previous timestamp was " + lastRow.timestamp()
                );
            }
        }
    }

    /**
     * Returns the ticker symbol associated with this batch.
     *
     * @return the ticker symbol, or {@code null} if the batch is empty
     */
    public String getTicker() {
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(0).ticker();
    }

    /**
     * Returns the number of candles in the batch.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return rows.size();
    }

    /**
     * Returns the {@code TimeInterval} shared by all rows in this DataBatch.
     * 
     * @return the {@code TimeInterval} of this DataBatch
     */
    public TimeInterval getInterval() {
        return interval;
    }

    /**
     * Returns the timestamp of the final candle in the batch.
     *
     * @return the timestamp of the last row, or {@code null} if empty
     */
    public Instant getFinalTimestamp() {
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(rows.size() - 1).timestamp();
    }

    /**
     * Returns whether the batch contains no rows.
     *
     * @return {@code true} if the batch is empty, otherwise {@code false}
     */
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
     * Converts the batch into a 2D array of OHLCV values.
     *
     * <p>The returned array has shape {@code [batchSize][5]}, where each row
     * corresponds to a single candle and columns are ordered as:
     *
     * <pre>
     * [open, high, low, close, volume]
     * </pre>
     *
     * <p>This format is suitable for feeding into machine learning models.
     *
     * @return a 2D array representation of the batch
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
     * @return a verbose string, meant for debugging
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
        Instant start = rows.get(0).timestamp();
        Instant end = getFinalTimestamp();

        return "DataBatch{" +
                "ticker='" + ticker + '\'' +
                ", interval=" + interval +
                ", size=" + size +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}