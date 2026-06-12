package org.example.domain;

import java.util.LinkedList;
import java.util.Queue;

public class ProductionQueue {

    private final Queue<Order> queue = new LinkedList<>();

    public void enqueue(Order order) {
        queue.add(order);
    }

    public int size() {
        return queue.size();
    }

    public boolean contains(String orderId) {
        for (Order order : queue) {
            if (order.getOrderId().equals(orderId)) {
                return true;
            }
        }
        return false;
    }
}
