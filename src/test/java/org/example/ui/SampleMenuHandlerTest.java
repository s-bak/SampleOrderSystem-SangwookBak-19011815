package org.example.ui;

import org.example.domain.Sample;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.domain.ProductionQueue;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

class SampleMenuHandlerTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream out;
    private SampleRepository sampleRepo;
    private SampleService sampleSvc;
    private JsonDataStore ds;
    private ProductionQueue queue;
    private OrderRepository orderRepo;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        sampleRepo = new SampleRepository();
        orderRepo = new OrderRepository();
        queue = new ProductionQueue();
        sampleSvc = new SampleService(sampleRepo);
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
    void handle_listAll_empty_showsMessage() {
        new SampleMenuHandler(io("2\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("등록된 시료가 없습니다"), output());
    }

    @Test
    void handle_listAll_withSamples_showsTable() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new SampleMenuHandler(io("2\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("S-001"), output());
        assertTrue(output().contains("AlphaChip"), output());
    }

    @Test
    void handle_register_success() {
        new SampleMenuHandler(io("1\nS-001\nAlphaChip\n30\n0.85\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("등록 완료"), output());
        assertTrue(sampleSvc.existsById("S-001"), output());
    }

    @Test
    void handle_register_emptyId_returnsToMenu() {
        new SampleMenuHandler(io("1\n\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_register_duplicateId_retries() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new SampleMenuHandler(io("1\nS-001\nS-002\nBetaChip\n20.0\n0.90\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("이미 존재하는"), output());
        assertTrue(sampleSvc.existsById("S-002"), output());
    }

    @Test
    void handle_register_emptyName_returnsToMenu() {
        new SampleMenuHandler(io("1\nS-001\n\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }

    @Test
    void handle_register_zeroAvgTime_showsError() {
        new SampleMenuHandler(io("1\nS-001\nAlphaChip\n0\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
        assertFalse(sampleSvc.existsById("S-001"), output());
    }

    @Test
    void handle_register_invalidYield_showsError() {
        new SampleMenuHandler(io("1\nS-001\nAlphaChip\n30\n0\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("[오류]"), output());
        assertFalse(sampleSvc.existsById("S-001"), output());
    }

    @Test
    void handle_search_byId_found() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new SampleMenuHandler(io("3\n1\n-001\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("S-001"), output());
    }

    @Test
    void handle_search_noResult_showsMessage() {
        sampleRepo.save(new Sample("S-001", "AlphaChip", 30.0, 0.85, 10));
        new SampleMenuHandler(io("3\n1\nXXX\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("검색 결과가 없습니다"), output());
    }

    @Test
    void handle_search_emptyKeyword_returnsToMenu() {
        new SampleMenuHandler(io("3\n1\n\n0\n"), sampleSvc, ds).handle();
        assertTrue(output().contains("[안내]"), output());
    }
}
