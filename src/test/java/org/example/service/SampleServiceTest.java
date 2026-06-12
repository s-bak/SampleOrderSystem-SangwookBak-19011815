package org.example.service;

import org.example.domain.Sample;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleServiceTest {

    private SampleService service;

    @BeforeEach
    void setUp() {
        service = new SampleService(new SampleRepository());
    }

    @Test
    void register_success() {
        Sample s = service.register("S-001", "AlphaChip", 30, 0.85, 100);
        assertEquals("S-001", s.getId());
        assertEquals("AlphaChip", s.getName());
        assertEquals(0.85, s.getYield());
        assertEquals(100, s.getStock());
    }

    @Test
    void register_duplicateId_throws() {
        service.register("S-001", "AlphaChip", 30, 0.85, 100);
        assertThrows(IllegalArgumentException.class,
                () -> service.register("S-001", "BetaChip", 20, 0.9, 50));
    }

    @Test
    void register_invalidYield_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("S-001", "AlphaChip", 30, 0.0, 100));
    }

    @Test
    void register_negativeStock_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("S-001", "AlphaChip", 30, 0.85, -1));
    }

    @Test
    void findAll_empty() {
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void findAll_returnsRegistered() {
        service.register("S-001", "AlphaChip", 30, 0.85, 100);
        service.register("S-002", "BetaChip", 20, 0.9, 50);
        List<Sample> all = service.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void search_match() {
        service.register("S-001", "AlphaChip", 30, 0.85, 100);
        List<Sample> result = service.search("lph");
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getId());
    }

    @Test
    void search_noMatch() {
        service.register("S-001", "AlphaChip", 30, 0.85, 100);
        assertTrue(service.search("xyz").isEmpty());
    }

    @Test
    void findById_success() {
        service.register("S-001", "AlphaChip", 30, 0.85, 100);
        Sample s = service.findById("S-001");
        assertEquals("S-001", s.getId());
    }

    @Test
    void findById_notFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findById("S-999"));
    }
}
