package org.example.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class ProductionQueue {

    private final Queue<ProductionJob> queue = new LinkedList<>();

    public void enqueue(Order order) {
        int shortfall = order.getQuantity() - order.getSample().getStock();
        boolean wasEmpty = queue.isEmpty();
        ProductionJob job = new ProductionJob(order, shortfall, LocalDateTime.now());
        queue.add(job);
        if (wasEmpty) {
            job.start(); // 큐가 비어 있었으면 즉시 생산 시작
        }
    }

    /** 현재 작업 완료 후 다음 대기 작업의 생산을 시작 */
    public void startNext() {
        ProductionJob next = queue.peek();
        if (next != null && !next.isActive()) {
            next.start();
        }
    }

    public Optional<ProductionJob> peek() {
        return Optional.ofNullable(queue.peek());
    }

    public List<ProductionJob> getWaiting() {
        List<ProductionJob> all = new ArrayList<>(queue);
        if (!all.isEmpty()) {
            all.remove(0); // 헤드(현재 생산 중)를 제외한 대기 목록
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

    public void enqueueJob(ProductionJob job) {
        queue.add(job);
    }

    public List<ProductionJob> getAll() {
        return new ArrayList<>(queue);
    }
}
