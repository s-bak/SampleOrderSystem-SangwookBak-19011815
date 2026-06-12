package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.service.MonitoringService;

import java.util.List;
import java.util.Map;

public class MonitoringMenuHandler {

    private final ConsoleIO io;
    private final MonitoringService monitoringService;

    public MonitoringMenuHandler(ConsoleIO io, MonitoringService monitoringService) {
        this.io = io;
        this.monitoringService = monitoringService;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 모니터링 ---");
            io.println("1. 주문량 확인 (상태별)");
            io.println("2. 재고량 확인");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> showOrdersByStatus();
                case "2" -> showStockStatus();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void showOrdersByStatus() {
        Map<OrderStatus, List<Order>> map = monitoringService.getOrdersByStatus();
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.RESERVED, OrderStatus.PRODUCING,
                OrderStatus.CONFIRMED, OrderStatus.RELEASE}) {
            List<Order> orders = map.get(status);
            io.println("\n[" + status + "] " + orders.size() + "건");
            if (!orders.isEmpty()) {
                io.println(String.format("  %-8s %-10s %-15s %6s", "주문ID", "고객명", "시료명", "수량"));
                io.println("  " + "-".repeat(44));
                for (Order o : orders) {
                    io.println(String.format("  %-8s %-10s %-15s %6d",
                            o.getOrderId(), o.getCustomerName(),
                            o.getSample().getName(), o.getQuantity()));
                }
            }
        }
    }

    private void showStockStatus() {
        List<MonitoringService.StockStatusEntry> entries = monitoringService.getStockStatus();
        if (entries.isEmpty()) {
            io.println("등록된 시료가 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-15s %6s %8s %6s",
                "시료ID", "시료명", "재고", "대기수량", "상태"));
        io.println("-".repeat(50));
        for (MonitoringService.StockStatusEntry e : entries) {
            io.println(String.format("%-8s %-15s %6d %8d %6s",
                    e.getSample().getId(), e.getSample().getName(),
                    e.getSample().getStock(), e.getPendingQuantity(), e.getStatus()));
        }
    }
}
