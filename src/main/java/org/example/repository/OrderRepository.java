package org.example.repository;

import org.example.domain.Order;
import org.example.domain.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderRepository {

    private final Map<String, Order> store = new LinkedHashMap<>();
    private int sequence = 0;

    public void save(Order order) {
        if (store.containsKey(order.getOrderId())) {
            throw new IllegalArgumentException("이미 존재하는 주문 ID입니다: " + order.getOrderId());
        }
        store.put(order.getOrderId(), order);
        sequence++;
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Order> findByStatus(OrderStatus status) {
        List<Order> result = new ArrayList<>();
        for (Order order : store.values()) {
            if (order.getStatus() == status) {
                result.add(order);
            }
        }
        return result;
    }

    public String generateNextId() {
        return String.format("O-%03d", sequence + 1);
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}
