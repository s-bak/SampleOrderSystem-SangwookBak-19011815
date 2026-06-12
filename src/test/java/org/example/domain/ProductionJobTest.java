package org.example.domain;

import org.junit.jupiter.api.Test;

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
        ProductionJob job = new ProductionJob(o, 5);
        assertEquals(7, job.getActualProductionCount());
    }

    @Test
    void calculate_actualCount_exact() {
        // shortfall=9, yield=1.0 → ceil(9 / (1.0×0.9)) = ceil(10.0) = 10
        Sample s = sample(1.0, 30);
        Order o = new Order("O-001", "고객A", s, 10);
        ProductionJob job = new ProductionJob(o, 9);
        assertEquals(10, job.getActualProductionCount());
    }

    @Test
    void calculate_totalTime() {
        // shortfall=5, yield=0.8, avgTime=30 → actualCount=7, totalTime=210
        Sample s = sample(0.8, 30);
        Order o = new Order("O-001", "고객A", s, 10);
        ProductionJob job = new ProductionJob(o, 5);
        assertEquals(210, job.getTotalProductionTime());
    }

    @Test
    void shortfall_zero_throws() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        assertThrows(IllegalArgumentException.class, () -> new ProductionJob(o, 0));
    }

    @Test
    void shortfall_negative_throws() {
        Sample s = sample(0.8, 30);
        Order o = order(s);
        assertThrows(IllegalArgumentException.class, () -> new ProductionJob(o, -1));
    }
}
