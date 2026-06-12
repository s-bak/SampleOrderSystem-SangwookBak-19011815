package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.service.ReleaseService;

import java.util.List;

public class ReleaseMenuHandler {

    private final ConsoleIO io;
    private final ReleaseService releaseService;
    private final OrderRepository orderRepository;
    private final JsonDataStore dataStore;

    public ReleaseMenuHandler(ConsoleIO io, ReleaseService releaseService, OrderRepository orderRepository, JsonDataStore dataStore) {
        this.io = io;
        this.releaseService = releaseService;
        this.orderRepository = orderRepository;
        this.dataStore = dataStore;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 출고 처리 ---");
            io.println("1. 출고 대기 주문 목록 조회");
            io.println("2. 출고 실행");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> listConfirmed();
                case "2" -> release();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private static final String RELEASE_SEP        = "+----------+------------+-----------------+--------+";
    private static final String RELEASE_HEADER_FMT = "| %-8s | %-10s | %-15s | %6s |";
    private static final String RELEASE_ROW_FMT    = "| %-8s | %-10s | %-15s | %6d |";

    private void listConfirmed() {
        List<Order> list = orderRepository.findByStatus(OrderStatus.CONFIRMED);
        if (list.isEmpty()) {
            io.println("출고 대기 중인 주문이 없습니다.");
            return;
        }
        io.println(RELEASE_SEP);
        io.println(String.format(RELEASE_HEADER_FMT, "주문ID", "고객명", "시료명", "수량"));
        io.println(RELEASE_SEP);
        for (Order o : list) {
            io.println(String.format(RELEASE_ROW_FMT,
                    o.getOrderId(), o.getCustomerName(), o.getSample().getName(), o.getQuantity()));
            io.println(RELEASE_SEP);
        }
    }

    private void release() {
        io.print("출고할 주문 ID: ");
        String orderId = io.readLine();
        try {
            releaseService.release(orderId);
            dataStore.save();
            io.println("출고 완료: [" + orderId + "]");
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }
}