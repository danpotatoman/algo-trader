package com.algotrader.analytics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class AnalyticsDatabaseInitializer {

    private static final Path DATABASE_PATH =
            Path.of("data", "analytics", "analytics.db");

    private AnalyticsDatabaseInitializer() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATABASE_PATH.getParent());

        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + DATABASE_PATH);
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS models (
                        model_id TEXT PRIMARY KEY,
                        creation_date TEXT NOT NULL,
                        prediction_type TEXT NOT NULL,
                        endpoint_url TEXT NOT NULL,
                        interval TEXT NOT NULL,
                        num_candles INTEGER NOT NULL,
                        description TEXT
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        session_id TEXT PRIMARY KEY,
                        model_id TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        initial_cash REAL NOT NULL,
                        final_cash REAL NOT NULL,
                        source_json_path TEXT NOT NULL,

                        FOREIGN KEY (model_id)
                            REFERENCES models(model_id)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS trades (
                        trade_id TEXT PRIMARY KEY,
                        session_id TEXT NOT NULL,
                        ticker TEXT NOT NULL,
                        action TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        price REAL NOT NULL,
                        quantity REAL NOT NULL,
                        cash_amount REAL NOT NULL,

                        FOREIGN KEY (session_id)
                            REFERENCES sessions(session_id)
                    )
                    """);
        }

        System.out.println("Analytics database initialized at " + DATABASE_PATH);
    }
}