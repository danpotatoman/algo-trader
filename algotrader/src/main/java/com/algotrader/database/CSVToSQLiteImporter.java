package com.algotrader.database;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.dataobjects.StampedOHLCV;
import com.algotrader.data.source.CSVOHLCVSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Meant to be a one-time importer to transition from CSV storage to SQL
 */
public class CSVToSQLiteImporter {

    private static final String CSV_DIRECTORY = "data/ohlcv";
    private static final String DATABASE_PATH = "data/ohlcv.db";

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

    private record ImportTarget(
            String ticker,
            TimeInterval interval
    ) {
    }
}