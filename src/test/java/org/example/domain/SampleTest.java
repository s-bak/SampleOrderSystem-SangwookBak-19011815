package org.example.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    private Sample create(double yield, int stock) {
        return new Sample("S-001", "TestSample", 10, yield, stock);
    }

    @Test
    void create_success() {
        Sample s = new Sample("S-001", "AlphaChip", 30, 0.85, 100);
        assertEquals("S-001", s.getId());
        assertEquals("AlphaChip", s.getName());
        assertEquals(30, s.getAvgProductionTime());
        assertEquals(0.85, s.getYield());
        assertEquals(100, s.getStock());
    }

    @Test
    void yield_zero_throws() {
        assertThrows(IllegalArgumentException.class, () -> create(0.0, 10));
    }

    @Test
    void yield_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> create(-0.1, 10));
    }

    @Test
    void yield_over_one_throws() {
        assertThrows(IllegalArgumentException.class, () -> create(1.01, 10));
    }

    @Test
    void yield_boundary_min() {
        assertDoesNotThrow(() -> create(0.01, 10));
    }

    @Test
    void yield_boundary_max() {
        assertDoesNotThrow(() -> create(1.0, 10));
    }

    @Test
    void negative_stock_throws() {
        assertThrows(IllegalArgumentException.class, () -> create(0.8, -1));
    }

    @Test
    void zero_stock_success() {
        assertDoesNotThrow(() -> create(0.8, 0));
    }

    @Test
    void avgProductionTime_zero_ok() {
        assertDoesNotThrow(() -> new Sample("S-001", "Test", 0.0, 0.8, 10));
    }

    @Test
    void avgProductionTime_decimal_ok() {
        assertDoesNotThrow(() -> new Sample("S-001", "Test", 0.5, 0.8, 10));
    }

    @Test
    void avgProductionTime_negative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Sample("S-001", "Test", -0.1, 0.8, 10));
    }

    @Test
    void decreaseStock_success() {
        Sample s = create(0.8, 10);
        s.decreaseStock(3);
        assertEquals(7, s.getStock());
    }

    @Test
    void decreaseStock_insufficient_throws() {
        Sample s = create(0.8, 2);
        assertThrows(IllegalStateException.class, () -> s.decreaseStock(3));
    }

    @Test
    void increaseStock_success() {
        Sample s = create(0.8, 5);
        s.increaseStock(4);
        assertEquals(9, s.getStock());
    }
}
