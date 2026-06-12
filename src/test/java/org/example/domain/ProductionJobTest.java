package org.example.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionJobTest {

    private Sample sample(double yield, int avgTime) {
        return new Sample("S-001", "AlphaChip", avgTime, yield, 0);
    }

    private Order order(Sample sample) {
        return new Order("O-001", "고객A", sample, 10);
    }

    @Test
    void calculate_actualCount() {
        // shortfall=5, yield=0.8 → ceil(5 / (0.8×0.9)) = ceil(5/0.72) = ceil(6.944) = 7
        Sample s = sample(0.8, 30);
        Order o = new Order("O-001", "고객A", s, 10);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        assertEquals(7, job.getActualProductionCount());
    }

    @Test
    void calculate_actualCount_exact() {
        // shortfall=9, yield=1.0 → ceil(9 / (1.0×0.9)) = ceil(10.0) = 10
        Sample s = sample(1.0, 30);
        Order o = new Order("O-001", "고객A", s, 10);
        ProductionJob job = new ProductionJob(o, 9, LocalDateTime.now());
        assertEquals(10, job.getActualProductionCount());
    }

    @Test
    void calculate_totalTime() {
        // shortfall=5, yield=0.8, avgTime=30 → actualCount=7, totalTime=210
        Sample s = sample(0.8, 30);
        Order o = new Order("O-001", "고객A", s, 10);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        assertEquals(210, job.getTotalProductionTime());
    }

    @Test
    void shortfall_zero_throws() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        assertThrows(IllegalArgumentException.class, () -> new ProductionJob(o, 0, LocalDateTime.now()));
    }

    @Test
    void shortfall_negative_throws() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        assertThrows(IllegalArgumentException.class, () -> new ProductionJob(o, -1, LocalDateTime.now()));
    }

    @Test
    void isActive_beforeStart_false() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        assertFalse(job.isActive());
        assertNull(job.getStartedAt());
    }

    @Test
    void isActive_afterStart_true() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        job.start();
        assertTrue(job.isActive());
        assertNotNull(job.getStartedAt());
    }

    @Test
    void addStock_accumulates() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        assertEquals(0, job.getStockAdded());
        job.addStock(2);
        assertEquals(2, job.getStockAdded());
        job.addStock(3);
        assertEquals(5, job.getStockAdded());
    }

    @Test
    void getRemainingShortfall_decreasesAsStockAdded() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        assertEquals(5, job.getRemainingShortfall());
        job.addStock(2);
        assertEquals(3, job.getRemainingShortfall());
        job.addStock(3);
        assertEquals(0, job.getRemainingShortfall());
    }

    @Test
    void restore_preservesStartedAtAndStockAdded() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        LocalDateTime enqueuedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime startedAt = LocalDateTime.of(2025, 1, 1, 10, 5);
        ProductionJob job = ProductionJob.restore(o, 5, enqueuedAt, startedAt, 3);
        assertEquals(enqueuedAt, job.getEnqueuedAt());
        assertEquals(startedAt, job.getStartedAt());
        assertEquals(3, job.getStockAdded());
        assertTrue(job.isActive());
    }

    @Test
    void restore_withNullStartedAt_notActive() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        LocalDateTime enqueuedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        ProductionJob job = ProductionJob.restore(o, 5, enqueuedAt, null, 0);
        assertFalse(job.isActive());
        assertNull(job.getStartedAt());
    }

    @Test
    void getEnqueuedAt_returnsCorrectly() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        LocalDateTime now = LocalDateTime.now();
        ProductionJob job = new ProductionJob(o, 5, now);
        assertEquals(now, job.getEnqueuedAt());
    }
}
