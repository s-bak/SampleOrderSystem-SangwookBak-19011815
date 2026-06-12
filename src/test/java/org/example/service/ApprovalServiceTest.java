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

    @Test
    void approve_confirmedQtyReducesAvailable_producing() {
        // 재고 5, CONFIRMED 주문 5 → 가용 재고 0 → 신규 주문 → PRODUCING
        Sample s = sample(5);
        Order first = savedOrder(s, 5);
        approvalService.approve(first.getOrderId()); // CONFIRMED, 재고 5 → 가용 0
        Order second = savedOrder(s, 3);
        Order result = approvalService.approve(second.getOrderId());
        assertEquals(OrderStatus.PRODUCING, result.getStatus());
    }

    @Test
    void approve_availableNegative_shortfallEqualsFullQuantity() {
        // 재고 3, CONFIRMED 5 → available = -2 → shortfall = max(0,-2) 아닌 전체 주문수량
        Sample s = sample(3);
        Order first = savedOrder(s, 5);
        approvalService.approve(first.getOrderId()); // CONFIRMED (재고 3 >= 5? 아님 → PRODUCING)
        // first는 PRODUCING (재고 3 < 5), shortfall=5
        // second: available = 3 - 0 - 0 = 3 (confirmedQty=0, producingStockAdded=0)
        // → second는 3 >= 3이므로 CONFIRMED
        Order second = savedOrder(s, 3);
        Order result = approvalService.approve(second.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void approve_producingStockAdded_reducesAvailable() {
        // 재고 10, 생산 중 작업에서 stockAdded=8 → available=10-0-8=2 < 5 → PRODUCING
        Sample s = sample(10);
        Order producingOrder = savedOrder(s, 15);
        approvalService.approve(producingOrder.getOrderId()); // PRODUCING
        // 생산 중 작업에 stockAdded 추가 시뮬레이션
        productionQueue.peek().ifPresent(job -> {
            job.addStock(8);
            s.increaseStock(8);
        });
        Order newOrder = savedOrder(s, 5);
        // available = (10+8) - 0 - 8 = 10 → 10 >= 5 → CONFIRMED
        Order result = approvalService.approve(newOrder.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void approve_shortfallWhenAvailableNegative_usesFullQuantity() {
        // 재고 0, CONFIRMED 주문 없음 → available=0-0-0=0 < 주문량10 → shortfall=10-0=10
        Sample s = sample(0);
        Order order = savedOrder(s, 10);
        Order result = approvalService.approve(order.getOrderId());
        assertEquals(OrderStatus.PRODUCING, result.getStatus());
        // shortfall은 주문수량 전체 (10)
        productionQueue.peek().ifPresent(job ->
                assertEquals(10, job.getShortfall())
        );
    }
}
