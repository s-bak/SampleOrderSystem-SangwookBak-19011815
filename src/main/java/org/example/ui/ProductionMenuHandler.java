package org.example.ui;

import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.service.ProductionService;

import java.util.List;
import java.util.Optional;

public class ProductionMenuHandler {

    private final ConsoleIO io;
    private final ProductionService productionService;
    private final ProductionQueue productionQueue;

    public ProductionMenuHandler(ConsoleIO io, ProductionService productionService, ProductionQueue productionQueue) {
        this.io = io;
        this.productionService = productionService;
        this.productionQueue = productionQueue;
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
        Optional<ProductionJob> current = productionQueue.peek();
        if (current.isEmpty()) {
            io.println("생산 중인 작업이 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-15s %8s %12s", "주문ID", "시료명", "실생산량", "총생산시간(min)"));
        io.println("-".repeat(48));
        printJobRow(current.get());
    }

    private void showWaiting() {
        List<ProductionJob> waiting = productionQueue.getWaiting();
        if (waiting.isEmpty()) {
            io.println("대기 중인 작업이 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-15s %8s %14s", "주문ID", "시료명", "실생산량", "예상생산시간(min)"));
        io.println("-".repeat(50));
        waiting.forEach(this::printJobRow);
    }

    private void complete() {
        io.print("완료 처리할 주문 ID: ");
        String orderId = io.readLine();
        try {
            var order = productionService.complete(orderId);
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
