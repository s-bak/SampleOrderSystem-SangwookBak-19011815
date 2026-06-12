package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionQueue;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringServiceTest {

    private SampleRepository sampleRepository;
    private OrderRepository orderRepository;
    private MonitoringService monitoringService;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        sampleRepository = new SampleRepository();
        orderRepository = new OrderRepository();
        monitoringService = new MonitoringService(sampleRepository, orderRepository);
        approvalService = new ApprovalService(orderRepository, new ProductionQueue());
    }

    private Sample registerSample(String id, int stock) {
        Sample s = new Sample(id, "Chip-" + id, 30, 0.85, stock);
        sampleRepository.save(s);
        return s;
    }

    private Order placeOrder(Sample sample, int quantity) {
        Order order = new Order(orderRepository.generateNextId(), "고객A", sample, quantity);
        orderRepository.save(order);
        return order;
    }

    @Test
    void getOrdersByStatus_allEmpty() {
        Map<OrderStatus, List<Order>> result = monitoringService.getOrdersByStatus();
        assertEquals(4, result.size());
        assertTrue(result.get(OrderStatus.RESERVED).isEmpty());
        assertTrue(result.get(OrderStatus.PRODUCING).isEmpty());
        assertTrue(result.get(OrderStatus.CONFIRMED).isEmpty());
        assertTrue(result.get(OrderStatus.RELEASE).isEmpty());
    }

    @Test
    void getOrdersByStatus_groupsCorrectly() {
        Sample s = registerSample("S-001", 100);
        Order o1 = placeOrder(s, 5);
        Order o2 = placeOrder(s, 3);
        Order o3 = placeOrder(s, 2);
        o3.transitionTo(OrderStatus.CONFIRMED);

        Map<OrderStatus, List<Order>> result = monitoringService.getOrdersByStatus();
        assertEquals(2, result.get(OrderStatus.RESERVED).size());
        assertEquals(1, result.get(OrderStatus.CONFIRMED).size());
        assertTrue(result.get(OrderStatus.PRODUCING).isEmpty());
    }

    @Test
    void getOrdersByStatus_excludesRejected() {
        Sample s = registerSample("S-001", 100);
        Order o = placeOrder(s, 5);
        o.transitionTo(OrderStatus.REJECTED);

        Map<OrderStatus, List<Order>> result = monitoringService.getOrdersByStatus();
        assertFalse(result.containsKey(OrderStatus.REJECTED));
        assertTrue(result.get(OrderStatus.RESERVED).isEmpty());
    }

    @Test
    void getStockStatus_여유() {
        Sample s = registerSample("S-001", 10);
        placeOrder(s, 5);

        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        assertEquals(1, entries.size());
        assertEquals("여유", entries.get(0).getStatus());
        assertEquals(5, entries.get(0).getPendingQuantity());
    }

    @Test
    void getStockStatus_부족() {
        Sample s = registerSample("S-001", 3);
        placeOrder(s, 5);

        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        assertEquals("부족", entries.get(0).getStatus());
    }

    @Test
    void getStockStatus_고갈() {
        registerSample("S-001", 0);

        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        assertEquals("고갈", entries.get(0).getStatus());
    }

    @Test
    void getStockStatus_noPending_여유() {
        registerSample("S-001", 5);

        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        assertEquals("여유", entries.get(0).getStatus());
        assertEquals(0, entries.get(0).getPendingQuantity());
    }

    @Test
    void getStockStatus_noPending_고갈() {
        registerSample("S-001", 0);

        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        assertEquals("고갈", entries.get(0).getStatus());
        assertEquals(0, entries.get(0).getPendingQuantity());
    }
}
