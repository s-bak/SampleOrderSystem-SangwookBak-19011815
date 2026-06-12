package org.example.ui;

import org.example.domain.Sample;
import org.example.service.ApprovalService;
import org.example.service.MonitoringService;
import org.example.service.OrderService;
import org.example.service.ProductionService;
import org.example.service.ReleaseService;
import org.example.service.SampleService;
import org.example.domain.ProductionQueue;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;

import java.util.List;

public class MainMenu {

    private final ConsoleIO io;
    private final SampleMenuHandler sampleHandler;
    private final OrderMenuHandler orderHandler;
    private final ApprovalMenuHandler approvalHandler;
    private final MonitoringMenuHandler monitoringHandler;
    private final ReleaseMenuHandler releaseHandler;
    private final ProductionMenuHandler productionHandler;
    private final SampleService sampleService;
    private final JsonDataStore dataStore;

    public MainMenu(ConsoleIO io,
                    SampleService sampleService,
                    OrderService orderService,
                    ApprovalService approvalService,
                    ProductionService productionService,
                    ReleaseService releaseService,
                    MonitoringService monitoringService,
                    ProductionQueue productionQueue,
                    OrderRepository orderRepository,
                    JsonDataStore dataStore) {
        this.io = io;
        this.sampleService = sampleService;
        this.sampleHandler = new SampleMenuHandler(io, sampleService);
        this.orderHandler = new OrderMenuHandler(io, orderService, sampleService);
        this.approvalHandler = new ApprovalMenuHandler(io, approvalService, orderRepository);
        this.monitoringHandler = new MonitoringMenuHandler(io, monitoringService);
        this.releaseHandler = new ReleaseMenuHandler(io, releaseService, orderRepository);
        this.productionHandler = new ProductionMenuHandler(io, productionService, productionQueue);
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            printHeader();
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> { sampleHandler.handle();    dataStore.save(); }
                case "2" -> { orderHandler.handle();     dataStore.save(); }
                case "3" -> { approvalHandler.handle();  dataStore.save(); }
                case "4" -> monitoringHandler.handle();
                case "5" -> { releaseHandler.handle();   dataStore.save(); }
                case "6" -> { productionHandler.handle(); dataStore.save(); }
                case "0" -> {
                    io.println("시스템을 종료합니다.");
                    return;
                }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void printHeader() {
        List<Sample> samples = sampleService.findAll();
        int totalStock = samples.stream().mapToInt(Sample::getStock).sum();
        io.println("\n==============================");
        io.println("  S-Semi 시료 생산주문관리 시스템");
        io.println("  등록 시료: " + samples.size() + "개 | 총 재고: " + totalStock + "개");
        io.println("==============================");
        io.println("1. 시료 관리");
        io.println("2. 주문 접수");
        io.println("3. 주문 승인 / 거절");
        io.println("4. 모니터링");
        io.println("5. 출고 처리");
        io.println("6. 생산 라인");
        io.println("0. 종료");
    }
}
