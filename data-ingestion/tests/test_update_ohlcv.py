import importlib.util
import sqlite3
import tempfile
import unittest
from contextlib import closing
from pathlib import Path
from unittest.mock import patch

import pandas as pd

spec = importlib.util.spec_from_file_location("ingestion", Path(__file__).resolve().parents[1] / "update_ohlcv.py")
ingestion = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ingestion)


class TimestampIngestionTest(unittest.TestCase):
    def frame(self, unit):
        dates = pd.date_range("2026-05-19T13:30:00Z", periods=3, freq="5min").as_unit(unit)
        frame = pd.DataFrame({"Open": [100., 101., 102.], "High": [101., 102., 103.],
                              "Low": [99., 100., 101.], "Close": [100., 101., 102.],
                              "Volume": [10, 20, 30]}, index=dates)
        frame.index.name = "Datetime"
        return frame

    def download(self, unit="s"):
        with patch.object(ingestion.yf, "download", return_value=self.frame(unit)):
            return ingestion.download_ohlcv("AAPL", "5m", "FIVE_MINUTES", "60d")

    def test_seconds_milliseconds_microseconds_nanoseconds_remain_distinct(self):
        expected = [int(d.timestamp()) for d in self.frame("s").index]
        for unit in ("s", "ms", "us", "ns"):
            with self.subTest(unit=unit):
                actual = self.download(unit)["timestamp"].tolist()
                self.assertEqual(actual, expected)
                self.assertEqual(len(set(actual)), 3)
                self.assertEqual(actual[1] - actual[0], 300)

    def test_actual_upsert_preserves_all_candles_and_existing_rows(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ohlcv.db"
            with closing(sqlite3.connect(path)) as con, con:
                con.execute("CREATE TABLE ohlcv (ticker TEXT, interval TEXT, timestamp INTEGER, open REAL, high REAL, low REAL, close REAL, volume REAL, PRIMARY KEY(ticker,interval,timestamp))")
                con.execute("INSERT INTO ohlcv VALUES ('OTHER','ONE_MINUTE',1779197400,1,1,1,1,1)")
            backup = ingestion.backup_database(path)
            self.assertEqual(ingestion.upsert_ohlcv(self.download(), path), 3)
            self.assertEqual(ingestion.upsert_ohlcv(self.download("ns"), path), 3)
            with closing(sqlite3.connect(path)) as con:
                self.assertEqual(con.execute("SELECT COUNT(*) FROM ohlcv").fetchone()[0], 4)
                self.assertEqual(con.execute("SELECT COUNT(DISTINCT timestamp) FROM ohlcv WHERE ticker='AAPL'").fetchone()[0], 3)
            with closing(sqlite3.connect(backup)) as con:
                self.assertEqual(con.execute("SELECT COUNT(*) FROM ohlcv").fetchone()[0], 1)

    def test_invalid_dates_and_collapsed_duplicates_rejected_before_writing(self):
        frame = self.download()
        for invalid in (1, 0, -1, None, 9999999999999, frame.timestamp.iloc[0] + 1):
            with self.subTest(timestamp=invalid), tempfile.TemporaryDirectory() as directory:
                broken = frame.copy()
                broken.loc[0, "timestamp"] = invalid
                path = Path(directory) / "must-not-exist.db"
                with self.assertRaises(ValueError):
                    ingestion.upsert_ohlcv(broken, path)
                self.assertFalse(path.exists())
        frame.loc[1, "timestamp"] = frame.timestamp.iloc[0]
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            ingestion.validate_timestamps(frame)

    def test_nat_rejected(self):
        with self.assertRaises(ValueError):
            ingestion.unix_seconds(pd.Series([pd.NaT]))

    def test_incomplete_candle_is_not_ingested(self):
        frame = self.frame("s")
        # At 13:36, the 13:35 and 13:40 candles are incomplete/future respectively.
        frame = frame.iloc[:2]
        from datetime import datetime, timezone
        with patch.object(ingestion.yf, "download", return_value=frame), patch.object(ingestion, "datetime") as clock:
            clock.now.return_value = datetime(2026, 5, 19, 13, 36, tzinfo=timezone.utc)
            result = ingestion.download_ohlcv("AAPL", "5m", "FIVE_MINUTES", "60d")
        self.assertEqual(len(result), 1)


if __name__ == "__main__":
    unittest.main()
