package org.example.domain;

public class Sample {

    private final String id;
    private final String name;
    private final double avgProductionTime;
    private final double yield;
    private int stock;

    public Sample(String id, String name, double avgProductionTime, double yield, int stock) {
        if (yield <= 0 || yield > 1) {
            throw new IllegalArgumentException("수율은 0 초과 1 이하여야 합니다.");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
        if (avgProductionTime <= 0) {
            throw new IllegalArgumentException("평균 생산시간은 0 초과여야 합니다.");
        }
        this.id = id;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public void decreaseStock(int amount) {
        if (amount > stock) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + stock + ", 요청: " + amount);
        }
        this.stock -= amount;
    }

    public void increaseStock(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        }
        this.stock += amount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getAvgProductionTime() { return avgProductionTime; }
    public double getYield() { return yield; }
    public int getStock() { return stock; }
}
