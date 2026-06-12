package org.example.ui;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.domain.ProductionQueue;
import org.example.service.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseMenuHandlerTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private ProductionQueue queue;
    private ReleaseService releaseSvc;
    private JsonDataStore ds;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        queue = new ProductionQueue();
        releaseSvc = new ReleaseService(orderRepo);
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
    void handle_listConfirmed_empty_showsMessage() {
        new ReleaseMenuHandler(io("1\n0\n"), releaseSvc, orderRepo, ds).handle();
        assertTrue(output().contains("출고 대기 중인 주문이 없습니다"), output());
    }

    @Test
    void handle_release_noConfirmed_showsError() {
        new ReleaseMenuHandler(io("2\n0\n"), releaseSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[오류]"), output());
    }

    @Test
    void handle_release_emptyInput_returnsToMenu() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order confirmed = Order.restore("O-001", "고객A", s, 5, OrderStatus.CONFIRMED, LocalDateTime.now());
        orderRepo.restoreFromDb(confirmed);
        new ReleaseMenuHandler(io("2\n\n0\n"), releaseSvc, orderRepo, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_release_success() {
        Sample s = new Sample("S-001", "AlphaChip", 30.0, 0.85, 10);
        sampleRepo.save(s);
        Order confirmed = Order.restore("O-001", "고객A", s, 5, OrderStatus.CONFIRMED, LocalDateTime.now());
        orderRepo.restoreFromDb(confirmed);
        new ReleaseMenuHandler(io("2\nO-001\n0\n"), releaseSvc, orderRepo, ds).handle();
        assertTrue(output().contains("출고 완료"), output());
    }
}
