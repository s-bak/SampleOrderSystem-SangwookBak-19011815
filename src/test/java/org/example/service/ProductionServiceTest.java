package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionQueue;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductionServiceTest {

    private SampleRepository sampleRepository;
    private OrderRepository orderRepository;
    private ProductionQueue productionQueue;
    private ApprovalService approvalService;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        sampleRepository = new SampleRepository();
        orderRepository = new OrderRepository();
        productionQueue = new ProductionQueue();
        approvalService = new ApprovalService(orderRepository, productionQueue);
        productionService = new ProductionService(orderRepository, productionQueue);
    }

    private Order producingOrder(int stock, int quantity) {
        Sample sample = new Sample("S-001", "AlphaChip", 30, 1.0, stock);
        sampleRepository.save(sample);
        Order order = new Order(orderRepository.generateNextId(), "고객A", sample, quantity);
        orderRepository.save(order);
        approvalService.approve(order.getOrderId());
        return order;
    }

    @Test
    void complete_success() {
        Order order = producingOrder(2, 10);
        Order result = productionService.complete(order.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void complete_stockIncreased() {
        // stock=2, quantity=10, shortfall=8
        // 완료 후 stock = 2(기존) + 8(부족분) = 10 — 주문 수량을 정확히 충족
        Order order = producingOrder(2, 10);
        productionService.complete(order.getOrderId());
        assertEquals(10, order.getSample().getStock());
    }

    @Test
    void complete_removedFromQueue() {
        Order order = producingOrder(0, 5);
        productionService.complete(order.getOrderId());
        assertEquals(0, productionQueue.size());
    }

    @Test
    void complete_notInQueue_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productionService.complete("O-999"));
    }

    @Test
    void complete_multipleInQueue() {
        Order first = producingOrder(0, 5);
        Sample s2 = new Sample("S-002", "BetaChip", 20, 0.9, 0);
        sampleRepository.save(s2);
        Order second = new Order(orderRepository.generateNextId(), "고객B", s2, 3);
        orderRepository.save(second);
        approvalService.approve(second.getOrderId());

        assertEquals(2, productionQueue.size());
        productionService.complete(first.getOrderId());
        assertEquals(1, productionQueue.size());
        assertTrue(productionQueue.contains(second.getOrderId()));
    }
}
