package org.example.ui;

import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.repository.JsonDataStore;
import org.example.service.ProductionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ProductionMenuHandler {

    private final ConsoleIO io;
    private final ProductionService productionService;
    private final ProductionQueue productionQueue;
    private final JsonDataStore dataStore;

    public ProductionMenuHandler(ConsoleIO io, ProductionService productionService, ProductionQueue productionQueue, JsonDataStore dataStore) {
        this.io = io;
        this.productionService = productionService;
        this.productionQueue = productionQueue;
        this.dataStore = dataStore;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 생산 라인 ---");
            io.println("1. 생산 현황 조회 (현재 생산 중)");
            io.println("2. 대기 주문 확인");
            io.println("3. 생산 완료 처리");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> showCurrent();
                case "2" -> showWaiting();
                case "3" -> complete();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void showCurrent() {
        Optional<ProductionJob> currentOpt = productionQueue.peek();
        if (currentOpt.isEmpty()) {
            io.println("생산 중인 작업이 없습니다.");
            return;
        }
        ProductionJob job = currentOpt.get();

        updateStockIncremental(job);

        io.println(String.format("%-10s %-15s %-8s %6s %8s %8s %8s %12s %8s",
                "주문번호", "시료명", "시료ID", "주문량", "재고수량", "부족수량", "실생산량", "소요시간(min)", "진행률(%)"));
        io.println("-".repeat(92));
        printCurrentJobRow(job);

        if (job.getRemainingUnits() == 0) {
            try {
                var completed = productionService.complete(job.getOrder().getOrderId());
                dataStore.save();
                io.println("[생산 완료] 재고 자동 업데이트: " + completed.getSample().getStock() + "개");
            } catch (Exception e) {
                io.println("[오류] " + e.getMessage());
            }
        }
    }

    // avgProductionTime 단위로 1개씩 생산 완료된 수량을 재고에 증분 반영
    private void updateStockIncremental(ProductionJob job) {
        double elapsedMs = Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis();
        double avgMs = job.getOrder().getSample().getAvgProductionTime() * 60_000;
        int unitsProduced = (int) Math.min(job.getActualProductionCount(), elapsedMs / avgMs);
        int newUnits = unitsProduced - job.getUnitsAdded();
        if (newUnits > 0) {
            job.getOrder().getSample().increaseStock(newUnits);
            job.recordUnitsAdded(newUnits);
            dataStore.save();
        }
    }

    private void printCurrentJobRow(ProductionJob job) {
        var order = job.getOrder();
        var sample = order.getSample();
        double progressPct = job.getActualProductionCount() == 0 ? 100.0
                : (double) job.getUnitsAdded() / job.getActualProductionCount() * 100.0;
        io.println(String.format("%-10s %-15s %-8s %6d %8d %8d %8d %12.1f %8.1f",
                order.getOrderId(),
                sample.getName(),
                sample.getId(),
                order.getQuantity(),
                sample.getStock(),
                job.getShortfall(),
                job.getActualProductionCount(),
                job.getTotalProductionTime(),
                progressPct));
    }

    private void showWaiting() {
        List<ProductionJob> waiting = productionQueue.getWaiting();
        if (waiting.isEmpty()) {
            io.println("대기 중인 작업이 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-15s %8s %14s", "주문ID", "시료명", "실생산수", "예상총생산시간(min)"));
        io.println("-".repeat(50));
        waiting.forEach(this::printJobRow);
    }

    private void complete() {
        io.print("완료 처리할 주문 ID: ");
        String orderId = io.readLine();
        try {
            var order = productionService.complete(orderId);
            dataStore.save();
            io.println("생산 완료: [" + orderId + "], 재고 " + order.getSample().getStock() + "개");
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }

    private void printJobRow(ProductionJob job) {
        io.println(String.format("%-8s %-15s %8d %14.1f",
                job.getOrder().getOrderId(),
                job.getOrder().getSample().getName(),
                job.getActualProductionCount(),
                job.getTotalProductionTime()));
    }
}