package org.example.ui;

import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.repository.JsonDataStore;
import org.example.service.ProductionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> showCurrent();
                case "2" -> showWaiting();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private void showCurrent() {
        Optional<ProductionJob> currentOpt = productionQueue.peek();
        if (currentOpt.isEmpty() || !currentOpt.get().isActive()) {
            io.println("생산 중인 작업이 없습니다.");
            return;
        }
        ProductionJob job = currentOpt.get();
        double progressPct = calcProgress(job);

        io.println(String.format("%-10s %-15s %-8s %6s %8s %8s %12s %8s",
                "주문번호", "시료명", "시료ID", "주문량", "부족수량", "실생산량", "소요시간(min)", "진행률(%)"));
        io.println("-".repeat(83));
        printCurrentJobRow(job, progressPct);

        if (progressPct >= 100.0) {
            try {
                var completed = productionService.complete(job.getOrder().getOrderId());
                dataStore.save();
                io.println("[생산 완료] 재고 자동 업데이트: " + completed.getSample().getStock() + "개");
            } catch (Exception e) {
                io.println("[오류] " + e.getMessage());
            }
        }
    }

    private double calcProgress(ProductionJob job) {
        double elapsedMs = Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis();
        return Math.min(100.0, elapsedMs / (job.getTotalProductionTime() * 60_000) * 100.0);
    }

    private void printCurrentJobRow(ProductionJob job, double progressPct) {
        var order = job.getOrder();
        io.println(String.format("%-10s %-15s %-8s %6d %8d %8d %12.1f %8.1f",
                order.getOrderId(),
                order.getSample().getName(),
                order.getSample().getId(),
                order.getQuantity(),
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
        io.println(String.format("%-8s %-15s %8s %14s %16s", "주문ID", "시료명", "실생산수", "예상총생산시간(min)", "접수시각"));
        io.println("-".repeat(67));
        waiting.forEach(this::printJobRow);
    }

    private void printJobRow(ProductionJob job) {
        io.println(String.format("%-8s %-15s %8d %14.1f %16s",
                job.getOrder().getOrderId(),
                job.getOrder().getSample().getName(),
                job.getActualProductionCount(),
                job.getTotalProductionTime(),
                job.getEnqueuedAt().format(DT_FMT)));
    }
}