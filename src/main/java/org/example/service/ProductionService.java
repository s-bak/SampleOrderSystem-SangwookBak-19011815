package org.example.service;

import org.example.domain.OrderStatus;
import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.domain.Order;
import org.example.repository.OrderRepository;

public class ProductionService {

    private final OrderRepository orderRepository;
    private final ProductionQueue productionQueue;

    public ProductionService(OrderRepository orderRepository, ProductionQueue productionQueue) {
        this.orderRepository = orderRepository;
        this.productionQueue = productionQueue;
    }

    public Order complete(String orderId) {
        ProductionJob job = productionQueue.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("생산 큐에 존재하지 않는 주문 ID입니다: " + orderId));
        Order order = job.getOrder();
        productionQueue.remove(orderId);
        order.transitionTo(OrderStatus.CONFIRMED);
        productionQueue.startNext();
        return order;
    }
}
