package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionQueue;
import org.example.domain.Sample;
import org.example.exception.InvalidOrderStateTransitionException;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalServiceTest {

    private OrderRepository orderRepository;
    private ProductionQueue productionQueue;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        productionQueue = new ProductionQueue();
        approvalService = new ApprovalService(orderRepository, productionQueue);
    }

    private Sample sample(int stock) {
        return new Sample("S-001", "AlphaChip", 30, 0.85, stock);
    }

    private Order savedOrder(Sample sample, int quantity) {
        Order order = new Order(orderRepository.generateNextId(), "고객A", sample, quantity);
        orderRepository.save(order);
        return order;
    }

    @Test
    void approve_stockSufficient_confirmed() {
        Sample s = sample(10);
        Order order = savedOrder(s, 5);
        Order result = approvalService.approve(order.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        // 재고 차감은 출고(RELEASE) 시점에 발생 — CONFIRMED 승인 시 재고 불변
        assertEquals(10, s.getStock());
    }

    @Test
    void approve_stockExact_confirmed() {
        Sample s = sample(5);
        Order order = savedOrder(s, 5);
        Order result = approvalService.approve(order.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(5, s.getStock());
    }

    @Test
    void approve_stockInsufficient_producing() {
        Sample s = sample(10);
        Order order = savedOrder(s, 15);
        Order result = approvalService.approve(order.getOrderId());
        assertEquals(OrderStatus.PRODUCING, result.getStatus());
        assertEquals(10, s.getStock());
        assertTrue(productionQueue.contains(order.getOrderId()));
    }

    @Test
    void approve_stockZero_producing() {
        Sample s = sample(0);
        Order order = savedOrder(s, 5);
        Order result = approvalService.approve(order.getOrderId());
        assertEquals(OrderStatus.PRODUCING, result.getStatus());
        assertTrue(productionQueue.contains(order.getOrderId()));
    }

    @Test
    void approve_notReserved_throws() {
        Sample s = sample(10);
        Order order = savedOrder(s, 5);
        approvalService.approve(order.getOrderId());
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> approvalService.approve(order.getOrderId()));
    }

    @Test
    void approve_orderNotFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> approvalService.approve("O-999"));
    }

    @Test
    void reject_success() {
        Sample s = sample(10);
        Order order = savedOrder(s, 5);
        Order result = approvalService.reject(order.getOrderId());
        assertEquals(OrderStatus.REJECTED, result.getStatus());
    }

    @Test
    void reject_notReserved_throws() {
        Sample s = sample(10);
        Order order = savedOrder(s, 5);
        approvalService.approve(order.getOrderId());
        assertThrows(InvalidOrderStateTransitionException.class,
                () -> approvalService.reject(order.getOrderId()));
    }

    @Test
    void reject_orderNotFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> approvalService.reject("O-999"));
    }
}
