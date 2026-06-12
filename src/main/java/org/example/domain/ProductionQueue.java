package org.example.domain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class ProductionQueue {

    private final Queue<ProductionJob> queue = new LinkedList<>();

    public void enqueue(Order order) {
        int shortfall = order.getQuantity() - order.getSample().getStock();
        queue.add(new ProductionJob(order, shortfall));
    }

    public Optional<ProductionJob> peek() {
        return Optional.ofNullable(queue.peek());
    }

    public List<ProductionJob> getWaiting() {
        List<ProductionJob> all = new ArrayList<>(queue);
        if (!all.isEmpty()) {
            all.remove(0);
        }
        return all;
    }

    public Optional<ProductionJob> findByOrderId(String orderId) {
        for (ProductionJob job : queue) {
            if (job.getOrder().getOrderId().equals(orderId)) {
                return Optional.of(job);
            }
        }
        return Optional.empty();
    }

    public boolean remove(String orderId) {
        return queue.removeIf(job -> job.getOrder().getOrderId().equals(orderId));
    }

    public boolean contains(String orderId) {
        return findByOrderId(orderId).isPresent();
    }

    public int size() {
        return queue.size();
    }
}
