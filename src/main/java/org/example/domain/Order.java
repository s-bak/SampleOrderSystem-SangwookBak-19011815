package org.example.domain;

import org.example.exception.InvalidOrderStateTransitionException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class Order {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.RESERVED, EnumSet.of(OrderStatus.REJECTED, OrderStatus.PRODUCING, OrderStatus.CONFIRMED),
            OrderStatus.PRODUCING, EnumSet.of(OrderStatus.CONFIRMED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.RELEASE),
            OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.RELEASE, EnumSet.noneOf(OrderStatus.class)
    );

    private final String orderId;
    private final String customerName;
    private final Sample sample;
    private final int quantity;
    private final LocalDateTime createdAt;
    private OrderStatus status;

    public Order(String orderId, String customerName, Sample sample, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
        this.orderId = orderId;
        this.customerName = customerName;
        this.sample = sample;
        this.quantity = quantity;
        this.status = OrderStatus.RESERVED;
        this.createdAt = LocalDateTime.now();
    }

    private Order(String orderId, String customerName, Sample sample, int quantity,
                  OrderStatus status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.sample = sample;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Order restore(String orderId, String customerName, Sample sample, int quantity,
                                OrderStatus status, LocalDateTime createdAt) {
        return new Order(orderId, customerName, sample, quantity, status, createdAt);
    }

    public void transitionTo(OrderStatus next) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(next)) {
            throw new InvalidOrderStateTransitionException(
                    status + " 상태에서 " + next + " 상태로 전이할 수 없습니다."
            );
        }
        this.status = next;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public Sample getSample() { return sample; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
