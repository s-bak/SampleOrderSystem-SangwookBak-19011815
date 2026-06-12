package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.service.MonitoringService;

import java.util.List;
import java.util.Map;

public class MonitoringMenuHandler {

    private static final char   ESC    = 0x1B;
    private static final String RESET  = ESC + "[0m";
    private static final String CYAN   = ESC + "[96m";        // 하늘색 (RESERVED)
    private static final String GREEN  = ESC + "[92m";        // 연두색 (CONFIRMED / 여유)
    private static final String ORANGE = ESC + "[38;5;214m";  // 주황색 (PRODUCING / 부족)
    private static final String PURPLE = ESC + "[95m";        // 연보라색 (RELEASE)
    private static final String RED    = ESC + "[91m";        // 빨강색 (고갈)

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
            String color = colorOf(status);
            io.println(color + "\n[" + status + "] " + orders.size() + "건" + RESET);
            if (!orders.isEmpty()) {
                io.println(color + String.format("  %-8s %-10s %-15s %6s", "주문ID", "고객명", "시료명", "수량") + RESET);
                io.println(color + "  " + "-".repeat(44) + RESET);
                for (Order o : orders) {
                    io.println(color + String.format("  %-8s %-10s %-15s %6d",
                            o.getOrderId(), o.getCustomerName(),
                            o.getSample().getName(), o.getQuantity()) + RESET);
                }
            }
        }
    }

    private String colorOf(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> CYAN;
            case CONFIRMED -> GREEN;
            case PRODUCING -> ORANGE;
            case RELEASE   -> PURPLE;
            default        -> "";
        };
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
            String color = stockColorOf(e.getStatus());
            io.println(color + String.format("%-8s %-15s %6d %8d %6s",
                    e.getSample().getId(), e.getSample().getName(),
                    e.getSample().getStock(), e.getPendingQuantity(), e.getStatus()) + RESET);
        }
    }

    private String stockColorOf(String status) {
        return switch (status) {
            case "고갈" -> RED;
            case "부족" -> ORANGE;
            case "여유" -> GREEN;
            default    -> "";
        };
    }
}
