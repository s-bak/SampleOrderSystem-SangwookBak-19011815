package org.example.repository;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderRepositoryTest {

    private OrderRepository repo;
    private Sample sample;

    @BeforeEach
    void setUp() {
        repo = new OrderRepository();
        sample = new Sample("S-001", "AlphaChip", 30, 0.85, 100);
    }

    private Order order(String id) {
        return new Order(id, "고객A", sample, 5);
    }

    @Test
    void save_and_findById() {
        Order o = order("O-001");
        repo.save(o);
        assertTrue(repo.findById("O-001").isPresent());
        assertEquals(o, repo.findById("O-001").get());
    }

    @Test
    void findById_notFound() {
        assertTrue(repo.findById("O-999").isEmpty());
    }

    @Test
    void findAll_empty() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void findAll_returnsAll() {
        repo.save(order("O-001"));
        repo.save(order("O-002"));
        repo.save(order("O-003"));
        List<Order> all = repo.findAll();
        assertEquals(3, all.size());
        assertEquals("O-001", all.get(0).getOrderId());
        assertEquals("O-002", all.get(1).getOrderId());
        assertEquals("O-003", all.get(2).getOrderId());
    }

    @Test
    void findByStatus_reserved() {
        Order o1 = order("O-001");
        Order o2 = order("O-002");
        Order o3 = order("O-003");
        o3.transitionTo(OrderStatus.CONFIRMED);
        repo.save(o1);
        repo.save(o2);
        repo.save(o3);
        List<Order> reserved = repo.findByStatus(OrderStatus.RESERVED);
        assertEquals(2, reserved.size());
    }

    @Test
    void findByStatus_noMatch() {
        repo.save(order("O-001"));
        assertTrue(repo.findByStatus(OrderStatus.CONFIRMED).isEmpty());
    }

    @Test
    void generateNextId_sequence() {
        assertEquals("O-001", repo.generateNextId());
        repo.save(order(repo.generateNextId()));
        assertEquals("O-002", repo.generateNextId());
    }

    @Test
    void save_duplicateId_throws() {
        repo.save(order("O-001"));
        assertThrows(IllegalArgumentException.class, () -> repo.save(order("O-001")));
    }

    @Test
    void restoreFromDb_storesWithoutIncrementingSequence() {
        Order o = order("O-005");
        repo.restoreFromDb(o);
        assertTrue(repo.findById("O-005").isPresent());
        // sequence는 증가하지 않아야 함
        assertEquals(0, repo.getSequence());
    }

    @Test
    void getSequence_and_setSequence() {
        assertEquals(0, repo.getSequence());
        repo.setSequence(10);
        assertEquals(10, repo.getSequence());
        assertEquals("O-011", repo.generateNextId());
    }

    @Test
    void restoreFromDb_overwritesExisting() {
        Order original = order("O-001");
        repo.restoreFromDb(original);
        Order replacement = new Order("O-001", "새고객", sample, 3);
        repo.restoreFromDb(replacement);
        assertEquals("새고객", repo.findById("O-001").get().getCustomerName());
    }
}
