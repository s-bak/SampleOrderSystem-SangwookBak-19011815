package org.example.domain;

import java.time.LocalDateTime;

public class ProductionJob {

    private final Order order;
    private final int actualProductionCount;
    private final double totalProductionTime;
    private final LocalDateTime startedAt;

    public ProductionJob(Order order, int shortfall, LocalDateTime startedAt) {
        if (shortfall < 1) {
            throw new IllegalArgumentException("부족분은 1 이상이어야 합니다.");
        }
        this.order = order;
        this.actualProductionCount = (int) Math.ceil(shortfall / (order.getSample().getYield() * 0.9));
        this.totalProductionTime = order.getSample().getAvgProductionTime() * actualProductionCount;
        this.startedAt = startedAt;
    }

    public Order getOrder() { return order; }
    public int getActualProductionCount() { return actualProductionCount; }
    public double getTotalProductionTime() { return totalProductionTime; }
    public LocalDateTime getStartedAt() { return startedAt; }
}
