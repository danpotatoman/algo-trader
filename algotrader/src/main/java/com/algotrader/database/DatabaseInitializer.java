package com.algotrader.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void main(String[] args) {

        String url = "jdbc:sqlite:data/ohlcv.db";

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

                PRIMARY KEY (
                    ticker,
                    interval,
                    timestamp
                )
            );
            """;

        try (
                Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()
        ) {

            stmt.execute(sql);

            System.out.println("Table created");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}