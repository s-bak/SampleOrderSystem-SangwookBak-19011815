package org.example.service;

import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.repository.JsonDataStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProductionScheduler {

    private final ProductionQueue productionQueue;
    private final ProductionService productionService;
    private final JsonDataStore dataStore;
    private final ScheduledExecutorService executor;

    public ProductionScheduler(ProductionQueue productionQueue,
                               ProductionService productionService,
                               JsonDataStore dataStore) {
        this.productionQueue = productionQueue;
        this.productionService = productionService;
        this.dataStore = dataStore;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "production-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        executor.scheduleAtFixedRate(this::checkAndComplete, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
    }

    private void checkAndComplete() {
        Optional<ProductionJob> current = productionQueue.peek();
        if (current.isEmpty() || !current.get().isActive()) return;

        ProductionJob job = current.get();
        double elapsedMs = Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis();

        // 단위당 생산 시간(ms) = 총 생산시간 / 재고에 추가할 수량(shortfall)
        double msPerUnit = job.getTotalProductionTime() * 60_000.0 / job.getShortfall();
        int expectedUnits = (int) Math.min(job.getShortfall(), elapsedMs / msPerUnit);
        int newUnits = expectedUnits - job.getStockAdded();

        if (newUnits > 0) {
            job.getOrder().getSample().increaseStock(newUnits);
            job.addStock(newUnits);
            dataStore.save();
        }

        if (job.getStockAdded() >= job.getShortfall()) {
            try {
                productionService.complete(job.getOrder().getOrderId());
                dataStore.save();
            } catch (Exception ignored) {
                // 이미 완료된 작업이거나 동시 접근으로 인한 중복 호출 무시
            }
        }
    }
}
