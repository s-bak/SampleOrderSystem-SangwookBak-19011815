package org.example.ui;

import org.example.domain.Order;
import org.example.domain.Sample;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.domain.ProductionQueue;
import org.example.service.OrderService;
import org.example.service.SampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMenuHandlerTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private SampleService sampleSvc;
    private OrderService orderSvc;
    private JsonDataStore ds;
    private ProductionQueue queue;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        queue = new ProductionQueue();
        sampleSvc = new SampleService(sampleRepo);
        orderSvc = new OrderService(sampleRepo, orderRepo);
        ds = new JsonDataStore(tempDir.resolve("db.json"), sampleRepo, orderRepo, queue);
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
    void handle_placeOrder_noSamples_showsError() {
        new OrderMenuHandler(io("1\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_placeOrder_success() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-001\n고객A\n5\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("주문 등록 완료"), output());
        assertTrue(output().contains("O-001"), output());
    }

    @Test
    void handle_placeOrder_emptyInput_returnsToMenu() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\n\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_placeOrder_invalidQuantity_showsError() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-001\n고객A\nabc\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_listAll_empty_showsMessage() {
        new OrderMenuHandler(io("2\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("주문 내역이 없습니다"), output());
    }

    @Test
    void handle_listAll_withOrders_showsTable() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new OrderMenuHandler(io("2\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("O-001"), output());
        assertTrue(output().contains("고객A"), output());
    }

    @Test
    void handle_mainMenu_invalidInput_showsError() {
        new OrderMenuHandler(io("X\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_placeOrder_invalidSampleId_retriesInput() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-999\nS-001\n고객A\n5\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("존재하지 않는 시료 ID"), output());
        assertTrue(output().contains("주문 등록 완료"), output());
    }

    @Test
    void handle_placeOrder_emptyCustomerName_returnsToMenu() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-001\n\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_placeOrder_emptyQuantity_returnsToMenu() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-001\n고객A\n\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_placeOrder_serviceError_showsError() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new OrderMenuHandler(io("1\nS-001\n고객A\n0\n0\n"), orderSvc, sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }
}
