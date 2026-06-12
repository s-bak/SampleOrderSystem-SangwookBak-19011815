package org.example.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductionQueueTest {

    private ProductionQueue queue;
    private Sample sample;

    @BeforeEach
    void setUp() {
        queue = new ProductionQueue();
        sample = new Sample("S-001", "AlphaChip", 30, 0.85, 0);
    }

    private Order order(String id) {
        return new Order(id, "고객A", sample, 10);
    }

    @Test
    void enqueue_emptyQueue_jobStartsImmediately() {
        Order o = order("O-001");
        queue.enqueue(o, 5);
        assertTrue(queue.peek().isPresent());
        assertTrue(queue.peek().get().isActive());
    }

    @Test
    void enqueue_nonEmptyQueue_secondJobWaits() {
        queue.enqueue(order("O-001"), 5);
        queue.enqueue(order("O-002"), 3);
        List<ProductionJob> waiting = queue.getWaiting();
        assertEquals(1, waiting.size());
        assertFalse(waiting.get(0).isActive());
    }

    @Test
    void enqueue_multipleJobs_onlyHeadIsActive() {
        queue.enqueue(order("O-001"), 5);
        queue.enqueue(order("O-002"), 3);
        queue.enqueue(order("O-003"), 7);
        assertTrue(queue.peek().get().isActive());
        for (ProductionJob job : queue.getWaiting()) {
            assertFalse(job.isActive());
        }
    }

    @Test
    void startNext_emptyQueue_noException() {
        assertDoesNotThrow(() -> queue.startNext());
    }

    @Test
    void startNext_headAlreadyActive_noDoubleStart() {
        queue.enqueue(order("O-001"), 5);
        LocalDateTime firstStartedAt = queue.peek().get().getStartedAt();
        // startNext는 이미 active인 헤드에는 start()를 호출하지 않음
        queue.startNext();
        assertEquals(firstStartedAt, queue.peek().get().getStartedAt());
    }

    @Test
    void startNext_activatesWaitingJob() {
        queue.enqueue(order("O-001"), 5);
        queue.enqueue(order("O-002"), 3);
        // O-001 제거 후 startNext 호출
        queue.remove("O-001");
        queue.startNext();
        assertTrue(queue.peek().get().isActive());
    }

    @Test
    void peek_emptyQueue_returnsEmpty() {
        assertTrue(queue.peek().isEmpty());
    }

    @Test
    void peek_nonEmptyQueue_returnsHead() {
        Order o = order("O-001");
        queue.enqueue(o, 5);
        Optional<ProductionJob> head = queue.peek();
        assertTrue(head.isPresent());
        assertEquals("O-001", head.get().getOrder().getOrderId());
    }

    @Test
    void getWaiting_emptyQueue_returnsEmpty() {
        assertTrue(queue.getWaiting().isEmpty());
    }

    @Test
    void getWaiting_singleItem_returnsEmpty() {
        queue.enqueue(order("O-001"), 5);
        assertTrue(queue.getWaiting().isEmpty());
    }

    @Test
    void getWaiting_multipleItems_excludesHead() {
        queue.enqueue(order("O-001"), 5);
        queue.enqueue(order("O-002"), 3);
        queue.enqueue(order("O-003"), 7);
        List<ProductionJob> waiting = queue.getWaiting();
        assertEquals(2, waiting.size());
        assertEquals("O-002", waiting.get(0).getOrder().getOrderId());
        assertEquals("O-003", waiting.get(1).getOrder().getOrderId());
    }

    @Test
    void findByOrderId_found() {
        queue.enqueue(order("O-001"), 5);
        Optional<ProductionJob> found = queue.findByOrderId("O-001");
        assertTrue(found.isPresent());
        assertEquals("O-001", found.get().getOrder().getOrderId());
    }

    @Test
    void findByOrderId_notFound() {
        queue.enqueue(order("O-001"), 5);
        assertTrue(queue.findByOrderId("O-999").isEmpty());
    }

    @Test
    void findByOrderId_emptyQueue_returnsEmpty() {
        assertTrue(queue.findByOrderId("O-001").isEmpty());
    }

    @Test
    void remove_success_returnsTrue() {
        queue.enqueue(order("O-001"), 5);
        assertTrue(queue.remove("O-001"));
        assertTrue(queue.peek().isEmpty());
    }

    @Test
    void remove_notFound_returnsFalse() {
        queue.enqueue(order("O-001"), 5);
        assertFalse(queue.remove("O-999"));
        assertEquals(1, queue.size());
    }

    @Test
    void contains_present_returnsTrue() {
        queue.enqueue(order("O-001"), 5);
        assertTrue(queue.contains("O-001"));
    }

    @Test
    void contains_absent_returnsFalse() {
        queue.enqueue(order("O-001"), 5);
        assertFalse(queue.contains("O-999"));
    }

    @Test
    void size_tracksCorrectly() {
        assertEquals(0, queue.size());
        queue.enqueue(order("O-001"), 5);
        assertEquals(1, queue.size());
        queue.enqueue(order("O-002"), 3);
        assertEquals(2, queue.size());
        queue.remove("O-001");
        assertEquals(1, queue.size());
    }

    @Test
    void enqueueJob_addsJobToQueue() {
        Order o = order("O-001");
        ProductionJob job = new ProductionJob(o, 5, LocalDateTime.now());
        queue.enqueueJob(job);
        assertEquals(1, queue.size());
        assertTrue(queue.contains("O-001"));
    }

    @Test
    void getAll_returnsAllJobs() {
        queue.enqueue(order("O-001"), 5);
        queue.enqueue(order("O-002"), 3);
        List<ProductionJob> all = queue.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void getAll_emptyQueue_returnsEmpty() {
        assertTrue(queue.getAll().isEmpty());
    }
}
