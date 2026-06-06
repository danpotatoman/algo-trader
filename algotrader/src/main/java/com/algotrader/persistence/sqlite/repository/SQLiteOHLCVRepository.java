package com.algotrader.persistence.sqlite.repository;

import com.algotrader.marketdata.model.OHLCV;
import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.persistence.OHLCVRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link OHLCVRepository}.
 *
 * <p>This repository persists {@link StampedOHLCV} candles in a local SQLite
 * database and provides lookup methods by ticker, interval, and timestamp.
 *
 * <p>The required {@code ohlcv} table is created automatically when the
 * repository is constructed. Candles are uniquely identified by ticker,
 * interval, and timestamp.
 *
 * <p>This class is the primary persistence implementation for historical
 * OHLCV market data.
 */
public class SQLiteOHLCVRepository implements OHLCVRepository {

    private final String databaseUrl;

    /**
     * Creates a SQLite OHLCV repository backed by the given database file.
     *
     * @param databasePath path to the SQLite database file
     */
    public SQLiteOHLCVRepository(String databasePath) {
        this.databaseUrl = "jdbc:sqlite:" + databasePath;
        initializeTable();
    }

    /**
     * Opens a new connection to the SQLite database.
     *
     * @return database connection
     * @throws SQLException if the connection cannot be opened
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    /**
     * Ensures that the OHLCV table exists.
     *
     * <p>This method is called during construction so callers do not need to run
     * a separate database initialization step.
     *
     * @throws RuntimeException if the table cannot be created
     */
    private void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS ohlcv (
                ticker TEXT NOT NULL,
                interval TEXT NOT NULL,
                timestamp INTEGER NOT NULL,

                open REAL NOT NULL,
                high REAL NOT NULL,
                low REAL NOT NULL,
                close REAL NOT NULL,
                volume REAL NOT NULL,

                PRIMARY KEY (ticker, interval, timestamp)
            );
            """;

        try (
                Connection conn = getConnection();
                Statement stmt = conn.createStatement()
        ) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize OHLCV table", e);
        }
    }

    /**
     * Saves one OHLCV candle.
     *
     * <p>If a candle already exists for the same ticker, interval, and timestamp,
     * the existing row is updated.
     *
     * @param candle candle to save
     * @throws RuntimeException if the candle cannot be saved
     */
    @Override
    public void save(StampedOHLCV candle) {
        String sql = """
            INSERT INTO ohlcv (
                ticker,
                interval,
                timestamp,
                open,
                high,
                low,
                close,
                volume
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(ticker, interval, timestamp)
            DO UPDATE SET
                open = excluded.open,
                high = excluded.high,
                low = excluded.low,
                close = excluded.close,
                volume = excluded.volume;
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            bindCandle(ps, candle);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save OHLCV candle", e);
        }
    }

    /**
     * Saves multiple OHLCV candles in a single batch transaction.
     *
     * <p>Existing rows with matching ticker, interval, and timestamp are updated.
     * If the supplied list is null or empty, this method does nothing.
     *
     * @param candles candles to save
     * @throws RuntimeException if the batch cannot be saved
     */
    @Override
    public void saveAll(List<StampedOHLCV> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO ohlcv (
                ticker,
                interval,
                timestamp,
                open,
                high,
                low,
                close,
                volume
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(ticker, interval, timestamp)
            DO UPDATE SET
                open = excluded.open,
                high = excluded.high,
                low = excluded.low,
                close = excluded.close,
                volume = excluded.volume;
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            conn.setAutoCommit(false);

            for (StampedOHLCV candle : candles) {
                bindCandle(ps, candle);
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save OHLCV candles", e);
        }
    }

    /**
     * Finds one candle by its full primary key.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param timestamp exact candle timestamp
     * @return matching candle, or {@link Optional#empty()} if none exists
     */
    @Override
    public Optional<StampedOHLCV> findByKey(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) {
        String sql = """
            SELECT *
            FROM ohlcv
            WHERE ticker = ?
              AND interval = ?
              AND timestamp = ?;
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, ticker);
            ps.setString(2, interval.name());
            ps.setLong(3, timestamp.getEpochSecond());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OHLCV candle", e);
        }
    }

    /**
     * Finds all candles within an inclusive timestamp range.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param start inclusive range start
     * @param end inclusive range end
     * @return matching candles ordered by ascending timestamp
     */
    @Override
    public List<StampedOHLCV> findRange(
            String ticker,
            TimeInterval interval,
            Instant start,
            Instant end
    ) {
        String sql = """
            SELECT *
            FROM ohlcv
            WHERE ticker = ?
              AND interval = ?
              AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp ASC;
            """;

        List<StampedOHLCV> results = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, ticker);
            ps.setString(2, interval.name());
            ps.setLong(3, start.getEpochSecond());
            ps.setLong(4, end.getEpochSecond());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OHLCV range", e);
        }
    }

    /**
     * Finds all candles for a ticker and interval.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @return matching candles ordered by ascending timestamp
     */
    @Override
    public List<StampedOHLCV> findAll(
            String ticker,
            TimeInterval interval
    ) {
        String sql = """
            SELECT *
            FROM ohlcv
            WHERE ticker = ?
            AND interval = ?
            ORDER BY timestamp ASC;
            """;

        List<StampedOHLCV> results = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, ticker);
            ps.setString(2, interval.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve OHLCV rows for "
                            + ticker + " " + interval,
                    e
            );
        }
    }

    /**
     * Finds the most recent candle for a ticker and interval.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @return latest candle, or {@link Optional#empty()} if none exists
     */
    @Override
    public Optional<StampedOHLCV> findLatest(
            String ticker,
            TimeInterval interval
    ) {
        String sql = """
            SELECT *
            FROM ohlcv
            WHERE ticker = ?
              AND interval = ?
            ORDER BY timestamp DESC
            LIMIT 1;
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, ticker);
            ps.setString(2, interval.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find latest OHLCV candle", e);
        }
    }

    /**
     * Finds the next available candle timestamp after the supplied timestamp.
     *
     * @param ticker ticker symbol to query
     * @param interval candle interval to query
     * @param timestamp timestamp after which to search
     * @return next available timestamp, or {@link Optional#empty()} if none exists
     */
    @Override
    public Optional<Instant> findNextTimestamp(
            String ticker,
            TimeInterval interval,
            Instant timestamp
    ) {
        String sql = """
            SELECT timestamp
            FROM ohlcv
            WHERE ticker = ?
            AND interval = ?
            AND timestamp > ?
            ORDER BY timestamp ASC
            LIMIT 1;
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, ticker);
            ps.setString(2, interval.name());
            ps.setLong(3, timestamp.getEpochSecond());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(
                            Instant.ofEpochSecond(rs.getLong("timestamp"))
                    );
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find next timestamp for "
                            + ticker + " " + interval
                            + " after " + timestamp,
                    e
            );
        }
    }

    /**
     * Binds a candle's values to an insert/update statement.
     *
     * @param ps prepared statement to bind values into
     * @param candle candle whose values should be bound
     * @throws SQLException if statement binding fails
     */
    private void bindCandle(
            PreparedStatement ps,
            StampedOHLCV candle
    ) throws SQLException {
        OHLCV ohlcv = candle.ohlcv();

        ps.setString(1, candle.ticker());
        ps.setString(2, candle.interval().name());
        ps.setLong(3, candle.timestamp().getEpochSecond());

        ps.setDouble(4, ohlcv.open());
        ps.setDouble(5, ohlcv.high());
        ps.setDouble(6, ohlcv.low());
        ps.setDouble(7, ohlcv.close());
        ps.setDouble(8, ohlcv.volume());
    }

    /**
     * Maps the current result-set row to a {@link StampedOHLCV}.
     *
     * @param rs result set positioned at a valid OHLCV row
     * @return mapped candle
     * @throws SQLException if row mapping fails
     */
    private StampedOHLCV mapRow(ResultSet rs) throws SQLException {
        OHLCV ohlcv = new OHLCV(
                rs.getDouble("open"),
                rs.getDouble("high"),
                rs.getDouble("low"),
                rs.getDouble("close"),
                rs.getLong("volume")
        );

        return new StampedOHLCV(
                rs.getString("ticker"),
                TimeInterval.valueOf(rs.getString("interval")),
                Instant.ofEpochSecond(rs.getLong("timestamp")),
                ohlcv
        );
    }
}