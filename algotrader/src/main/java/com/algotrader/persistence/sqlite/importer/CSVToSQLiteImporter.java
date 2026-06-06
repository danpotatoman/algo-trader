package com.algotrader.persistence.sqlite.importer;

import com.algotrader.marketdata.model.StampedOHLCV;
import com.algotrader.marketdata.model.TimeInterval;
import com.algotrader.marketdata.source.CSVOHLCVSource;
import com.algotrader.persistence.OHLCVRepository;
import com.algotrader.persistence.sqlite.repository.SQLiteOHLCVRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Manual utility for importing historical OHLCV CSV files into SQLite.
 *
 * <p>This class is intended as a one-off migration tool for transitioning
 * market data from CSV-based storage into the SQLite-backed persistence
 * layer.
 *
 * <p>The importer scans the configured CSV directory for files matching:
 *
 * <pre>
 * &lt;ticker&gt;_&lt;interval&gt;_ohlcv.csv
 * </pre>
 *
 * <p>For each matching file, it parses the ticker and interval from the file
 * name, loads the candles through {@link CSVOHLCVSource}, and writes them to
 * an {@link OHLCVRepository}.
 *
 * <p>This class is not part of the normal trading runtime.
 *
 * <p><b>Legacy status:</b> This importer depends on the legacy CSV loading
 * path and can be removed once CSV-to-SQLite migration is no longer needed.
 */
public class CSVToSQLiteImporter {

    private static final String CSV_DIRECTORY = "data/ohlcv";
    private static final String DATABASE_PATH = "data/ohlcv.db";

    /**
     * Runs the CSV-to-SQLite import process.
     *
     * @param args command-line arguments; currently unused
     */
    public static void main(String[] args) {
        CSVOHLCVSource csvSource = new CSVOHLCVSource(CSV_DIRECTORY);
        OHLCVRepository repository = new SQLiteOHLCVRepository(DATABASE_PATH);

        try {
            List<ImportTarget> targets = discoverImportTargets(Path.of(CSV_DIRECTORY));

            System.out.println("Found " + targets.size() + " CSV files to import.");

            for (ImportTarget target : targets) {
                System.out.println("Importing " + target.ticker()
                        + " " + target.interval());

                List<StampedOHLCV> candles =
                        csvSource.loadRows(
                                target.ticker(),
                                target.interval()
                        );

                repository.saveAll(candles);

                System.out.println("Imported " + candles.size() + " rows.");
            }

            System.out.println("CSV to SQLite import complete.");

        } catch (Exception e) {
            throw new RuntimeException("CSV to SQLite import failed", e);
        }
    }

    /**
     * Discovers OHLCV CSV files in the supplied directory and converts their
     * filenames into import targets.
     *
     * @param directory directory containing OHLCV CSV files
     * @return import targets discovered from matching filenames
     * @throws IOException if the directory cannot be listed
     */
    private static List<ImportTarget> discoverImportTargets(Path directory)
            throws IOException {

        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .endsWith("_ohlcv.csv"))
                    .map(CSVToSQLiteImporter::parseImportTarget)
                    .toList();
        }
    }

    /**
     * Parses a CSV filename into an import target.
     *
     * <p>Expected filename format:
     *
     * <pre>
     * aapl_5m_ohlcv.csv
     * </pre>
     *
     * @param path path to an OHLCV CSV file
     * @return import target containing ticker and interval
     * @throws IllegalArgumentException if the filename does not match the
     *         expected format
     */
    private static ImportTarget parseImportTarget(Path path) {
        String filename = path.getFileName().toString();

        // Example:
        // aapl_5m_ohlcv.csv
        String baseName = filename.replace("_ohlcv.csv", "");

        int underscoreIndex = baseName.lastIndexOf("_");

        if (underscoreIndex < 0) {
            throw new IllegalArgumentException(
                    "Invalid OHLCV CSV filename: " + filename
            );
        }

        String ticker = baseName
                .substring(0, underscoreIndex)
                .toUpperCase();

        String intervalToken = baseName
                .substring(underscoreIndex + 1);

        TimeInterval interval = parseInterval(intervalToken);

        return new ImportTarget(ticker, interval);
    }

    /**
     * Converts a filename interval token into a {@link TimeInterval}.
     *
     * @param token interval token such as {@code 1m}, {@code 5m}, or {@code 1d}
     * @return matching time interval
     * @throws IllegalArgumentException if the token is unsupported
     */
    private static TimeInterval parseInterval(String token) {
        return switch (token.toLowerCase()) {
            case "1m" -> TimeInterval.ONE_MINUTE;
            case "5m" -> TimeInterval.FIVE_MINUTES;
            case "15m" -> TimeInterval.FIFTEEN_MINUTES;
            case "30m" -> TimeInterval.THIRTY_MINUTES;
            case "1h" -> TimeInterval.ONE_HOUR;
            case "1d" -> TimeInterval.ONE_DAY;
            default -> throw new IllegalArgumentException(
                    "Unsupported interval token: " + token
            );
        };
    }

    /**
     * Import target derived from an OHLCV CSV filename.
     *
     * @param ticker ticker symbol to import
     * @param interval interval represented by the CSV file
     */
    private record ImportTarget(
            String ticker,
            TimeInterval interval
    ) {
    }
}