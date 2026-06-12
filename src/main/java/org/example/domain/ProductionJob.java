package org.example.domain;

import java.time.LocalDateTime;

public class ProductionJob {

    private final Order order;
    private final int shortfall;
    private final int actualProductionCount;
    private final double totalProductionTime;
    private final LocalDateTime enqueuedAt;
    private LocalDateTime startedAt; // null이면 대기 중, non-null이면 생산 중
    private int stockAdded = 0;      // 지금까지 재고에 추가된 수량

    public ProductionJob(Order order, int shortfall, LocalDateTime enqueuedAt) {
        if (shortfall < 1) {
            throw new IllegalArgumentException("부족분은 1 이상이어야 합니다.");
        }
        this.order = order;
        this.shortfall = shortfall;
        this.actualProductionCount = (int) Math.ceil(shortfall / (order.getSample().getYield() * 0.9));
        this.totalProductionTime = order.getSample().getAvgProductionTime() * actualProductionCount;
        this.enqueuedAt = enqueuedAt;
        this.startedAt = null;
    }

    public static ProductionJob restore(Order order, int shortfall,
                                        LocalDateTime enqueuedAt, LocalDateTime startedAt,
                                        int stockAdded) {
        ProductionJob job = new ProductionJob(order, shortfall, enqueuedAt);
        job.startedAt = startedAt;
        job.stockAdded = stockAdded;
        return job;
    }

    /** 대기 상태에서 생산 시작 — 큐 헤드가 됐을 때 호출 */
    public void start() {
        this.startedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return startedAt != null;
    }

    public void addStock(int amount) { this.stockAdded += amount; }
    public int getStockAdded() { return stockAdded; }
    public int getRemainingShortfall() { return shortfall - stockAdded; }

    public Order getOrder() { return order; }
    public int getShortfall() { return shortfall; }
    public int getActualProductionCount() { return actualProductionCount; }
    public double getTotalProductionTime() { return totalProductionTime; }
    public LocalDateTime getEnqueuedAt() { return enqueuedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
}
