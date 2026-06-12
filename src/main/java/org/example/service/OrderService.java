package org.example.service;

import org.example.domain.Order;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.util.List;

public class OrderService {

    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;

    public OrderService(SampleRepository sampleRepository, OrderRepository orderRepository) {
        this.sampleRepository = sampleRepository;
        this.orderRepository = orderRepository;
    }

    public Order placeOrder(String sampleId, String customerName, int quantity) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료 ID입니다: " + sampleId));
        if (quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
        String orderId = orderRepository.generateNextId();
        Order order = new Order(orderId, customerName, sample, quantity);
        orderRepository.save(order);
        return order;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
    }
}
