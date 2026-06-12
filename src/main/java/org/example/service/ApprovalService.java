package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionQueue;
import org.example.repository.OrderRepository;

public class ApprovalService {

    private final OrderRepository orderRepository;
    private final ProductionQueue productionQueue;

    public ApprovalService(OrderRepository orderRepository, ProductionQueue productionQueue) {
        this.orderRepository = orderRepository;
        this.productionQueue = productionQueue;
    }

    public Order approve(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));

        if (order.getSample().getStock() >= order.getQuantity()) {
            order.getSample().decreaseStock(order.getQuantity());
            order.transitionTo(OrderStatus.CONFIRMED);
        } else {
            productionQueue.enqueue(order);
            order.transitionTo(OrderStatus.PRODUCING);
        }
        return order;
    }

    public Order reject(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        order.transitionTo(OrderStatus.REJECTED);
        return order;
    }
}
