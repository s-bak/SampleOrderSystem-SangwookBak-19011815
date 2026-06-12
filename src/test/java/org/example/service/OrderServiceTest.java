package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private SampleRepository sampleRepository;
    private OrderService orderService;
    private Sample sample;

    @BeforeEach
    void setUp() {
        sampleRepository = new SampleRepository();
        orderService = new OrderService(sampleRepository, new OrderRepository());
        sample = new Sample("S-001", "AlphaChip", 30, 0.85, 100);
        sampleRepository.save(sample);
    }

    @Test
    void placeOrder_success() {
        Order order = orderService.placeOrder("S-001", "홍길동", 10);
        assertEquals("O-001", order.getOrderId());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(sample, order.getSample());
        assertEquals(10, order.getQuantity());
    }

    @Test
    void placeOrder_idSequence() {
        Order first = orderService.placeOrder("S-001", "고객A", 5);
        Order second = orderService.placeOrder("S-001", "고객B", 3);
        assertEquals("O-001", first.getOrderId());
        assertEquals("O-002", second.getOrderId());
    }

    @Test
    void placeOrder_sampleNotFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder("S-999", "홍길동", 5));
    }

    @Test
    void placeOrder_quantityZero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder("S-001", "홍길동", 0));
    }

    @Test
    void placeOrder_quantityNegative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder("S-001", "홍길동", -1));
    }

    @Test
    void findAll_empty() {
        assertTrue(orderService.findAll().isEmpty());
    }

    @Test
    void findAll_returnsAll() {
        orderService.placeOrder("S-001", "고객A", 5);
        orderService.placeOrder("S-001", "고객B", 3);
        List<Order> all = orderService.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void findById_success() {
        Order placed = orderService.placeOrder("S-001", "홍길동", 5);
        Order found = orderService.findById(placed.getOrderId());
        assertEquals(placed, found);
    }

    @Test
    void findById_notFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.findById("O-999"));
    }
}
