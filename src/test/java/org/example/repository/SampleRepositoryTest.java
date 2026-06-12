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
}
