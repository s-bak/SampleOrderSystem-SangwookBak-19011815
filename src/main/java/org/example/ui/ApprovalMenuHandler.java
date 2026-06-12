package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.service.ApprovalService;

import java.util.List;

public class ApprovalMenuHandler {

    private final ConsoleIO io;
    private final ApprovalService approvalService;
    private final OrderRepository orderRepository;
    private final JsonDataStore dataStore;

    public ApprovalMenuHandler(ConsoleIO io, ApprovalService approvalService, OrderRepository orderRepository, JsonDataStore dataStore) {
        this.io = io;
        this.approvalService = approvalService;
        this.orderRepository = orderRepository;
        this.dataStore = dataStore;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 주문 승인 / 거절 ---");
            io.println("1. 접수 주문 목록 조회");
            io.println("2. 거절 주문 목록 조회");
            io.println("3. 주문 승인");
            io.println("4. 주문 거절");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> listReserved();
                case "2" -> listRejected();
                case "3" -> approve();
                case "4" -> reject();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void listReserved() {
        List<Order> list = orderRepository.findByStatus(OrderStatus.RESERVED);
        if (list.isEmpty()) {
            io.println("대기 중인 주문이 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-10s %-15s %6s %s",
                "주문ID", "고객명", "시료명", "수량", "등록일시"));
        io.println("-".repeat(60));
        for (Order o : list) {
            io.println(String.format("%-8s %-10s %-15s %6d %s",
                    o.getOrderId(), o.getCustomerName(), o.getSample().getName(),
                    o.getQuantity(), o.getCreatedAt().toString().replace("T", " ").substring(0, 19)));
        }
    }

    private void listRejected() {
        List<Order> list = orderRepository.findByStatus(OrderStatus.REJECTED);
        if (list.isEmpty()) {
            io.println("거절된 주문이 없습니다.");
            return;
        }
        io.println(String.format("%-8s %-10s %-15s %6s %s",
                "주문ID", "고객명", "시료명", "수량", "등록일시"));
        io.println("-".repeat(60));
        for (Order o : list) {
            io.println(String.format("%-8s %-10s %-15s %6d %s",
                    o.getOrderId(), o.getCustomerName(), o.getSample().getName(),
                    o.getQuantity(), o.getCreatedAt().toString().replace("T", " ").substring(0, 19)));
        }
    }

    private void approve() {
        if (orderRepository.findByStatus(OrderStatus.RESERVED).isEmpty()) {
            io.println("[오류] 대기 중인 주문이 없습니다.");
            return;
        }
        io.print("승인할 주문 ID: ");
        String orderId = io.readLine();
        try {
            Order order = approvalService.approve(orderId);
            dataStore.save();
            io.println("승인 완료: [" + orderId + "] → " + order.getStatus());
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }

    private void reject() {
        if (orderRepository.findByStatus(OrderStatus.RESERVED).isEmpty()) {
            io.println("[오류] 대기 중인 주문이 없습니다.");
            return;
        }
        io.print("거절할 주문 ID: ");
        String orderId = io.readLine();
        try {
            approvalService.reject(orderId);
            dataStore.save();
            io.println("거절 완료: [" + orderId + "]");
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }
}