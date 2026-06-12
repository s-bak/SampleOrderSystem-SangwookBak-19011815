package org.example.domain;

public class ProductionJob {

    private final Order order;
    private final int actualProductionCount;
    private final double totalProductionTime;

    public ProductionJob(Order order, int shortfall) {
        if (shortfall < 1) {
            throw new IllegalArgumentException("부족분은 1 이상이어야 합니다.");
        }
        this.order = order;
        this.actualProductionCount = (int) Math.ceil(shortfall / (order.getSample().getYield() * 0.9));
        this.totalProductionTime = order.getSample().getAvgProductionTime() * actualProductionCount;
    }

    public Order getOrder() { return order; }
    public int getActualProductionCount() { return actualProductionCount; }
    public double getTotalProductionTime() { return totalProductionTime; }
}
