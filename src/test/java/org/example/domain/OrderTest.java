package org.example.domain;

import org.example.exception.InvalidOrderStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Sample sample;

    @BeforeEach
    void setUp() {
        sample = new Sample("S-001", "AlphaChip", 30, 0.85, 100);
    }

    private Order createOrder() {
        return new Order("O-001", "홍길동", sample, 10);
    }

    @Test
    void create_success() {
        Order order = createOrder();
        assertEquals("O-001", order.getOrderId());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(sample, order.getSample());
        assertEquals(10, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void create_invalidQuantity_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("O-001", "홍길동", sample, 0));
    }

    @Test
    void create_negativeQuantity_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("O-001", "홍길동", sample, -1));
    }

    @Test
    void transition_reserved_to_rejected() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.REJECTED);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void transition_reserved_to_producing() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.PRODUCING);
        assertEquals(OrderStatus.PRODUCING, order.getStatus());
    }

    @Test
    void transition_reserved_to_confirmed() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void transition_producing_to_confirmed() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.PRODUCING);
        order.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void transition_confirmed_to_release() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.RELEASE);
        assertEquals(OrderStatus.RELEASE, order.getStatus());
    }

    @Test
    void transition_reserved_to_release_throws() {
        Order order = createOrder();
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.RELEASE));
    }

    @Test
    void transition_rejected_throws() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.REJECTED);
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.RESERVED));
    }

    @Test
    void transition_release_throws() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.RELEASE);
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void transition_producing_to_rejected_throws() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.PRODUCING);
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.REJECTED));
    }

    @Test
    void transition_confirmed_to_producing_throws() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.PRODUCING));
    }

    @Test
    void transition_confirmed_to_rejected_throws() {
        Order order = createOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.REJECTED));
    }

    @Test
    void transition_reserved_to_self_throws() {
        Order order = createOrder();
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> order.transitionTo(OrderStatus.RESERVED));
    }

    @Test
    void restore_createsOrderWithGivenStatus() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        Order restored = Order.restore("O-999", "복원고객", sample, 7,
                OrderStatus.CONFIRMED, createdAt);
        assertEquals("O-999", restored.getOrderId());
        assertEquals("복원고객", restored.getCustomerName());
        assertEquals(sample, restored.getSample());
        assertEquals(7, restored.getQuantity());
        assertEquals(OrderStatus.CONFIRMED, restored.getStatus());
        assertEquals(createdAt, restored.getCreatedAt());
    }
}
