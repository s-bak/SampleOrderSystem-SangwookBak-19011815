package org.example.ui;

import org.example.domain.Order;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.MonitoringService;
import org.example.domain.ProductionQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringMenuHandlerTest {

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private MonitoringService monitorSvc;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        monitorSvc = new MonitoringService(sampleRepo, orderRepo);
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
    void handle_showOrdersByStatus_empty_showsZeroCounts() {
        new MonitoringMenuHandler(io("1\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("RESERVED"), output());
        assertTrue(output().contains("0건"), output());
    }

    @Test
    void handle_showOrdersByStatus_withOrders_showsData() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new MonitoringMenuHandler(io("1\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("1건"), output());
    }

    @Test
    void handle_showStockStatus_empty_showsMessage() {
        new MonitoringMenuHandler(io("2\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("등록된 시료가 없습니다"), output());
    }

    @Test
    void handle_showStockStatus_여유_showsTable() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new MonitoringMenuHandler(io("2\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("S-001"), output());
        assertTrue(output().contains("여유"), output());
    }

    @Test
    void handle_mainMenu_invalidInput_showsError() {
        new MonitoringMenuHandler(io("X\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_showStockStatus_부족_showsStatus() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new MonitoringMenuHandler(io("2\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("부족"), output());
    }

    @Test
    void handle_showStockStatus_고갈_showsStatus() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 0));
        new MonitoringMenuHandler(io("2\n0\n"), monitorSvc).handle();
        assertTrue(output().contains("고갈"), output());
    }
}
