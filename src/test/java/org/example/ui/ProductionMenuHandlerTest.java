package org.example.ui;

import org.example.domain.Order;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.domain.ProductionQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionMenuHandlerTest {

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private ProductionQueue queue;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        queue = new ProductionQueue();
    }

    private ConsoleIO io(String inputs) {
        return new ConsoleIO(
            new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8)
        );
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void handle_showCurrent_noActiveJob_showsMessage() {
        new ProductionMenuHandler(io("1\n0\n"), queue).handle();
        assertTrue(output().contains("생산 중인 작업이 없습니다"), output());
    }

    @Test
    void handle_showCurrent_withActiveJob_showsDetails() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 0);
        sampleRepo.save(s);
        Order o1 = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o1);
        queue.enqueue(o1, 5);  // 큐가 비어 있었으므로 즉시 start() 호출
        new ProductionMenuHandler(io("1\n0\n"), queue).handle();
        assertTrue(output().contains("O-001"), output());
        assertTrue(output().contains("AlphaChip"), output());
    }

    @Test
    void handle_showWaiting_empty_showsMessage() {
        new ProductionMenuHandler(io("2\n0\n"), queue).handle();
        assertTrue(output().contains("대기 중인 작업이 없습니다"), output());
    }

    @Test
    void handle_showWaiting_withJobs_showsTable() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 0);
        sampleRepo.save(s);
        Order o1 = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o1);
        queue.enqueue(o1, 5);  // 즉시 start() 호출 → 활성 상태
        Order o2 = new Order(orderRepo.generateNextId(), "고객B", s, 3);
        orderRepo.save(o2);
        queue.enqueue(o2, 3);  // 대기 상태
        new ProductionMenuHandler(io("2\n0\n"), queue).handle();
        assertTrue(output().contains("O-002"), output());
    }

    @Test
    void handle_mainMenu_invalidInput_showsError() {
        new ProductionMenuHandler(io("X\n0\n"), queue).handle();
        assertTrue(output().contains("[오류]"), output());
    }
}
