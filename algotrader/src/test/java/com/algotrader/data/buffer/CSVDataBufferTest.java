package com.algotrader.data.buffer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.algotrader.data.TimeInterval;
import com.algotrader.data.cache.DataBufferException;
import com.algotrader.data.dataobjects.DataBatch;

class CSVDataBufferTest {

    private CSVDataBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new CSVDataBuffer("test-data");
    }

    @Test
    void testRequestBatch_basic() throws Exception {
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Working Directory: " + currentDirectory);
        DataBatch batch = buffer.requestBatch(
                "AAPL",
                TimeInterval.FIVE_MINUTES,
                5
        );

        assertNotNull(batch);
        assertEquals(5, batch.getBatchSize());
        assertEquals("AAPL", batch.getTicker());
    }

    @Test
    void testSlidingWindow_advances() throws Exception {
        DataBatch batch1 = buffer.requestBatch("AAPL", TimeInterval.FIVE_MINUTES, 3);
        DataBatch batch2 = buffer.requestBatch("AAPL", TimeInterval.FIVE_MINUTES, 3);

        // They should not be identical (shifted by 1)
        assertNotEquals(
                batch1.getFinalTimestamp(),
                batch2.getFinalTimestamp()
        );
    }

    @Test
    void testRequestBatch_withTimestamp() throws Exception {
        String timeISO = "2026-04-24T16:15:00Z"; //ISO 8601 time known to be accessible to this CSVDataBuffer
        Instant ts = Instant.parse(timeISO);

        DataBatch batch = buffer.requestBatch(
                "AAPL",
                TimeInterval.FIVE_MINUTES,
                3,
                ts
        );

        assertEquals(ts, batch.getFinalTimestamp());
    }

    @Test
    void testInvalidTicker_throws() {
        assertThrows(DataBufferException.class, () -> {
            buffer.requestBatch("", TimeInterval.FIVE_MINUTES, 5);
        });
    }
}