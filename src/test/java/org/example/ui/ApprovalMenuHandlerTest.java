package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.domain.ProductionQueue;
import org.example.service.ApprovalService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalMenuHandlerTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private ProductionQueue queue;
    private ApprovalService approvalSvc;
    private JsonDataStore ds;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        queue = new ProductionQueue();
        approvalSvc = new ApprovalService(orderRepo, queue);
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
    void handle_listReserved_empty_showsMessage() {
        new ApprovalMenuHandler(io("1\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("대기 중인 주문이 없습니다"), output());
    }

    @Test
    void handle_listRejected_empty_showsMessage() {
        new ApprovalMenuHandler(io("2\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("거절된 주문이 없습니다"), output());
    }

    @Test
    void handle_approve_noReserved_showsError() {
        new ApprovalMenuHandler(io("3\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_approve_emptyInput_returnsToMenu() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("3\n\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_approve_stockSufficient_confirmed() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("3\nO-001\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("CONFIRMED"), output());
    }

    @Test
    void handle_approve_stockInsufficient_producing() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 0);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("3\nO-001\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("PRODUCING"), output());
        assertTrue(queue.size() == 1, output());
    }

    @Test
    void handle_reject_success() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("4\nO-001\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("거절 완료"), output());
    }

    @Test
    void handle_reject_emptyInput_returnsToMenu() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("4\n\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_mainMenu_invalidInput_showsError() {
        new ApprovalMenuHandler(io("X\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_listReserved_withOrders_showsTable() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("1\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("O-001"), output());
        assertTrue(output().contains("고객A"), output());
    }

    @Test
    void handle_listRejected_withOrders_showsTable() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order rejected = Order.restore("O-001", "고객A", s, 5, OrderStatus.REJECTED, LocalDateTime.now());
        orderRepo.restoreFromDb(rejected);
        new ApprovalMenuHandler(io("2\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("O-001"), output());
    }

    @Test
    void handle_approve_invalidOrderId_showsError() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("3\nO-999\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_reject_noReserved_showsError() {
        new ApprovalMenuHandler(io("4\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_reject_invalidOrderId_showsError() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order o = new Order(orderRepo.generateNextId(), "고객A", s, 5);
        orderRepo.save(o);
        new ApprovalMenuHandler(io("4\nO-999\n0\n"), approvalSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }
}
