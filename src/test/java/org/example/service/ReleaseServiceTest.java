package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.example.exception.InvalidOrderStateTransitionException;
import org.example.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseServiceTest {

    private OrderRepository orderRepository;
    private ReleaseService releaseService;
    private Sample sample;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        releaseService = new ReleaseService(orderRepository);
        sample = new Sample("S-001", "AlphaChip", 30, 0.85, 100);
    }

    private Order savedOrder() {
        Order order = new Order(orderRepository.generateNextId(), "고객A", sample, 5);
        orderRepository.save(order);
        return order;
    }

    @Test
    void release_success() {
        Order order = savedOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        Order result = releaseService.release(order.getOrderId());
        assertEquals(OrderStatus.RELEASE, result.getStatus());
    }

    @Test
    void release_notConfirmed_throws() {
        Order order = savedOrder();
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> releaseService.release(order.getOrderId()));
    }

    @Test
    void release_orderNotFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> releaseService.release("O-999"));
    }
}
