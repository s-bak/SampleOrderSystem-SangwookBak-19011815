package org.example.ui;

import org.example.domain.Order;
import org.example.service.OrderService;
import org.example.service.SampleService;

import java.util.List;

public class OrderMenuHandler {

    private final ConsoleIO io;
    private final OrderService orderService;
    private final SampleService sampleService;

    public OrderMenuHandler(ConsoleIO io, OrderService orderService, SampleService sampleService) {
        this.io = io;
        this.orderService = orderService;
        this.sampleService = sampleService;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 주문 접수 ---");
            io.println("1. 주문 등록");
            io.println("2. 주문 목록 조회");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> placeOrder();
                case "2" -> listAll();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void placeOrder() {
        String sampleId;
        while (true) {
            io.print("시료 ID: ");
            sampleId = io.readLine();
            if (sampleService.existsById(sampleId)) {
                break;
            }
            io.println("[오류] 존재하지 않는 시료 ID입니다: " + sampleId);
        }
        io.print("고객명: ");
        String customerName = io.readLine();
        int quantity = io.readInt("주문 수량: ");
        try {
            Order order = orderService.placeOrder(sampleId, customerName, quantity);
            io.println("주문 등록 완료: [" + order.getOrderId() + "] " + customerName + " / " + order.getSample().getName() + " x" + quantity);
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }

    private void listAll() {
        List<Order> list = orderService.findAll();
        if (list.isEmpty()) {
            io.println("주문 내역이 없습니다.");
            return;
        }
        printHeader();
        list.forEach(this::printRow);
    }

    private void printHeader() {
        io.println(String.format("%-8s %-10s %-15s %6s %-12s %s",
                "주문ID", "고객명", "시료명", "수량", "상태", "등록일시"));
        io.println("-".repeat(72));
    }

    private void printRow(Order o) {
        io.println(String.format("%-8s %-10s %-15s %6d %-12s %s",
                o.getOrderId(), o.getCustomerName(), o.getSample().getName(),
                o.getQuantity(), o.getStatus(), o.getCreatedAt().toString().replace("T", " ").substring(0, 19)));
    }
}
