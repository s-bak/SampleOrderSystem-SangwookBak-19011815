package org.example.ui;

import org.example.domain.Order;
import org.example.repository.JsonDataStore;
import org.example.service.OrderService;
import org.example.service.SampleService;

import java.util.List;

public class OrderMenuHandler {

    private final ConsoleIO io;
    private final OrderService orderService;
    private final SampleService sampleService;
    private final JsonDataStore dataStore;

    public OrderMenuHandler(ConsoleIO io, OrderService orderService, SampleService sampleService, JsonDataStore dataStore) {
        this.io = io;
        this.orderService = orderService;
        this.sampleService = sampleService;
        this.dataStore = dataStore;
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
        String customerName;
        while (true) {
            io.print("고객명: ");
            customerName = io.readLine();
            if (!customerName.isBlank()) break;
            io.println("[오류] 고객명은 공백일 수 없습니다.");
        }
        int quantity = io.readInt("주문 수량: ");
        try {
            Order order = orderService.placeOrder(sampleId, customerName, quantity);
            dataStore.save();
            io.println("주문 등록 완료: [" + order.getOrderId() + "] " + customerName + " / " + order.getSample().getName() + " x" + quantity);
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }

    private static final String ORDER_SEP        = "+----------+------------+------------------------+--------+--------------+---------------------+";
    private static final String ORDER_HEADER_FMT = "| %-8s | %-10s | %-22s | %6s | %-12s | %-19s |";
    private static final String ORDER_ROW_FMT    = "| %-8s | %-10s | %-22s | %6d | %-12s | %-19s |";

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
        io.println(ORDER_SEP);
        io.println(String.format(ORDER_HEADER_FMT, "주문ID", "고객명", "시료명 (ID)", "수량", "상태", "등록일시"));
        io.println(ORDER_SEP);
    }

    private void printRow(Order o) {
        String sampleCell = o.getSample().getName() + " (" + o.getSample().getId() + ")";
        io.println(String.format(ORDER_ROW_FMT,
                o.getOrderId(), o.getCustomerName(), sampleCell,
                o.getQuantity(), o.getStatus(), o.getCreatedAt().toString().replace("T", " ").substring(0, 19)));
        io.println(ORDER_SEP);
    }
}
