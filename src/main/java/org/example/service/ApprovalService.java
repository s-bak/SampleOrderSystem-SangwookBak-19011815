package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionJob;
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

        String sampleId = order.getSample().getId();

        // CONFIRMED 주문이 이미 점유한 수량
        int confirmedQty = orderRepository.findByStatus(OrderStatus.CONFIRMED)
                .stream()
                .filter(o -> o.getSample().getId().equals(sampleId))
                .mapToInt(Order::getQuantity)
                .sum();

        // 생산 중인 작업이 이미 재고에 추가한 수량 (해당 물량도 해당 주문에 귀속)
        int producingStockAdded = productionQueue.getAll()
                .stream()
                .filter(job -> job.getOrder().getSample().getId().equals(sampleId))
                .mapToInt(ProductionJob::getStockAdded)
                .sum();

        int available = order.getSample().getStock() - confirmedQty - producingStockAdded;

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
