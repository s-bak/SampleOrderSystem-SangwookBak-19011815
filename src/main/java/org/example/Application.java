package org.example;

import org.example.domain.ProductionQueue;
import org.example.repository.JsonDataStore;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.ApprovalService;
import org.example.service.MonitoringService;
import org.example.service.OrderService;
import org.example.service.ProductionService;
import org.example.service.ReleaseService;
import org.example.service.SampleService;
import org.example.ui.ConsoleIO;
import org.example.ui.MainMenu;

import java.nio.file.Paths;

public class Application {

    public static void main(String[] args) {
        SampleRepository sampleRepo = new SampleRepository();
        OrderRepository orderRepo = new OrderRepository();
        ProductionQueue queue = new ProductionQueue();

        JsonDataStore dataStore = new JsonDataStore(
                Paths.get("data", "db.json"),
                sampleRepo, orderRepo, queue);
        dataStore.load();

        SampleService sampleSvc = new SampleService(sampleRepo);
        OrderService orderSvc = new OrderService(sampleRepo, orderRepo);
        ApprovalService approvalSvc = new ApprovalService(orderRepo, queue);
        ProductionService productionSvc = new ProductionService(orderRepo, queue);
        ReleaseService releaseSvc = new ReleaseService(orderRepo);
        MonitoringService monitorSvc = new MonitoringService(sampleRepo, orderRepo);

        ConsoleIO io = new ConsoleIO(System.in, System.out);
        new MainMenu(io, sampleSvc, orderSvc, approvalSvc, productionSvc,
                releaseSvc, monitorSvc, queue, orderRepo, dataStore).run();
    }
}
