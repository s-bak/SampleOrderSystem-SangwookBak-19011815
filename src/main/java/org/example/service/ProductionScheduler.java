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
        double progress = Math.min(100.0, elapsedMs / (job.getTotalProductionTime() * 60_000) * 100.0);

        if (progress >= 100.0) {
            try {
                productionService.complete(job.getOrder().getOrderId());
                dataStore.save();
            } catch (Exception ignored) {
                // 이미 완료된 작업이거나 동시 접근으로 인한 중복 호출 무시
            }
        }
    }
}
