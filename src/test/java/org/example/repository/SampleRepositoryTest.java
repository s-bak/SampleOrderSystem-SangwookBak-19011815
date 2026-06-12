package org.example.repository;

import org.example.domain.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleRepositoryTest {

    private SampleRepository repo;

    @BeforeEach
    void setUp() {
        repo = new SampleRepository();
    }

    private Sample sample(String id, String name) {
        return new Sample(id, name, 10, 0.8, 50);
    }

    @Test
    void save_and_findById() {
        Sample s = sample("S-001", "Alpha");
        repo.save(s);
        assertTrue(repo.findById("S-001").isPresent());
        assertEquals(s, repo.findById("S-001").get());
    }

    @Test
    void findById_notFound() {
        assertTrue(repo.findById("S-999").isEmpty());
    }

    @Test
    void save_duplicateId_throws() {
        repo.save(sample("S-001", "Alpha"));
        assertThrows(IllegalArgumentException.class, () -> repo.save(sample("S-001", "Beta")));
    }

    @Test
    void findAll_empty() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void findAll_returnsAll() {
        repo.save(sample("S-001", "Alpha"));
        repo.save(sample("S-002", "Beta"));
        repo.save(sample("S-003", "Gamma"));
        List<Sample> all = repo.findAll();
        assertEquals(3, all.size());
        assertEquals("S-001", all.get(0).getId());
        assertEquals("S-002", all.get(1).getId());
        assertEquals("S-003", all.get(2).getId());
    }

    @Test
    void findByNameContaining_match() {
        repo.save(sample("S-001", "AlphaChip"));
        List<Sample> result = repo.findByNameContaining("lph");
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getId());
    }

    @Test
    void findByNameContaining_noMatch() {
        repo.save(sample("S-001", "AlphaChip"));
        assertTrue(repo.findByNameContaining("xyz").isEmpty());
    }

    @Test
    void findByNameContaining_multiMatch() {
        repo.save(sample("S-001", "AlphaX"));
        repo.save(sample("S-002", "AlphaY"));
        repo.save(sample("S-003", "BetaZ"));
        List<Sample> result = repo.findByNameContaining("Alpha");
        assertEquals(2, result.size());
    }

    @Test
    void findByIdContaining_match() {
        repo.save(sample("S-001", "Alpha"));
        repo.save(sample("S-002", "Beta"));
        List<Sample> result = repo.findByIdContaining("001");
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getId());
    }

    @Test
    void findByIdContaining_noMatch() {
        repo.save(sample("S-001", "Alpha"));
        assertTrue(repo.findByIdContaining("999").isEmpty());
    }

    @Test
    void findByAvgProductionTime_match() {
        repo.save(new Sample("S-001", "Alpha", 10.0, 0.8, 0));
        repo.save(new Sample("S-002", "Beta", 20.5, 0.9, 0));
        List<Sample> result = repo.findByAvgProductionTime(10.0);
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getId());
    }

    @Test
    void findByAvgProductionTime_noMatch() {
        repo.save(new Sample("S-001", "Alpha", 10.0, 0.8, 0));
        assertTrue(repo.findByAvgProductionTime(99.0).isEmpty());
    }

    @Test
    void findByYield_match() {
        repo.save(new Sample("S-001", "Alpha", 10.0, 0.9, 0));
        repo.save(new Sample("S-002", "Beta", 20.0, 0.8, 0));
        // 0.9와 0.90은 수학적으로 동일
        List<Sample> result = repo.findByYield(0.90);
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getId());
    }

    @Test
    void findByYield_noMatch() {
        repo.save(new Sample("S-001", "Alpha", 10.0, 0.8, 0));
        assertTrue(repo.findByYield(0.5).isEmpty());
    }
}
