package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.repository.OrderRepository;

public class ReleaseService {

    private final OrderRepository orderRepository;

    public ReleaseService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order release(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        order.getSample().decreaseStock(order.getQuantity());
        order.transitionTo(OrderStatus.RELEASE);
        return order;
    }
}
