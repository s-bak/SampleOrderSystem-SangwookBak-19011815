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

        // CONFIRMED 주문이 이미 점유한 수량을 제외한 가용 재고
        int confirmedQty = orderRepository.findByStatus(OrderStatus.CONFIRMED)
                .stream()
                .filter(o -> o.getSample().getId().equals(order.getSample().getId()))
                .mapToInt(Order::getQuantity)
                .sum();
        int available = order.getSample().getStock() - confirmedQty;

        if (available >= order.getQuantity()) {
            order.transitionTo(OrderStatus.CONFIRMED);
        } else {
            int shortfall = order.getQuantity() - Math.max(0, available);
            productionQueue.enqueue(order, shortfall);
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
