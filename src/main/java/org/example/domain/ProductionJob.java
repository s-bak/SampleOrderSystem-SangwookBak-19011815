package org.example.domain;

import java.time.LocalDateTime;

public class ProductionJob {

    private final Order order;
    private final int shortfall;
    private final int actualProductionCount;
    private final double totalProductionTime;
    private final LocalDateTime startedAt;
    private int unitsAdded;

    public ProductionJob(Order order, int shortfall, LocalDateTime startedAt) {
        if (shortfall < 1) {
            throw new IllegalArgumentException("부족분은 1 이상이어야 합니다.");
        }
        this.order = order;
        this.shortfall = shortfall;
        this.actualProductionCount = (int) Math.ceil(shortfall / (order.getSample().getYield() * 0.9));
        this.totalProductionTime = order.getSample().getAvgProductionTime() * actualProductionCount;
        this.startedAt = startedAt;
        this.unitsAdded = 0;
    }

    public static ProductionJob restore(Order order, int shortfall, LocalDateTime startedAt, int unitsAdded) {
        ProductionJob job = new ProductionJob(order, shortfall, startedAt);
        job.unitsAdded = unitsAdded;
        return job;
    }

    public void recordUnitsAdded(int n) {
        this.unitsAdded += n;
    }

    public int getRemainingUnits() {
        return actualProductionCount - unitsAdded;
    }

    public Order getOrder() { return order; }
    public int getShortfall() { return shortfall; }
    public int getActualProductionCount() { return actualProductionCount; }
    public double getTotalProductionTime() { return totalProductionTime; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public int getUnitsAdded() { return unitsAdded; }
}
