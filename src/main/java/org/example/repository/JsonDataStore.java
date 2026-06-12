package org.example.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.ProductionJob;
import org.example.domain.ProductionQueue;
import org.example.domain.Sample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JsonDataStore {

    private final Path dbPath;
    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;
    private final ProductionQueue productionQueue;
    private final ObjectMapper mapper;

    public JsonDataStore(Path dbPath,
                         SampleRepository sampleRepository,
                         OrderRepository orderRepository,
                         ProductionQueue productionQueue) {
        this.dbPath = dbPath;
        this.sampleRepository = sampleRepository;
        this.orderRepository = orderRepository;
        this.productionQueue = productionQueue;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void load() {
        if (!Files.exists(dbPath)) return;
        try {
            DbSnapshot snapshot = mapper.readValue(dbPath.toFile(), DbSnapshot.class);

            for (SampleDto dto : snapshot.samples) {
                sampleRepository.save(new Sample(dto.id, dto.name, dto.avgProductionTime, dto.yield, dto.stock));
            }

            orderRepository.setSequence(snapshot.orderSequence);
            for (OrderDto dto : snapshot.orders) {
                Sample sample = sampleRepository.findById(dto.sampleId)
                        .orElseThrow(() -> new IllegalStateException("DB 오류: 시료를 찾을 수 없습니다. id=" + dto.sampleId));
                Order order = Order.restore(
                        dto.orderId, dto.customerName, sample, dto.quantity,
                        OrderStatus.valueOf(dto.status), LocalDateTime.parse(dto.createdAt));
                orderRepository.save(order);
            }

            for (QueueItemDto dto : snapshot.productionQueue) {
                Order order = orderRepository.findById(dto.orderId)
                        .orElseThrow(() -> new IllegalStateException("DB 오류: 주문을 찾을 수 없습니다. id=" + dto.orderId));
                LocalDateTime startedAt = dto.startedAt != null
                        ? LocalDateTime.parse(dto.startedAt) : LocalDateTime.now();
                productionQueue.enqueueJob(ProductionJob.restore(order, dto.shortfall, startedAt, dto.unitsAdded));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("DB 로드 실패: " + dbPath, e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(dbPath.getParent());
            DbSnapshot snapshot = new DbSnapshot();

            for (Sample s : sampleRepository.findAll()) {
                SampleDto dto = new SampleDto();
                dto.id = s.getId();
                dto.name = s.getName();
                dto.avgProductionTime = s.getAvgProductionTime();
                dto.yield = s.getYield();
                dto.stock = s.getStock();
                snapshot.samples.add(dto);
            }

            snapshot.orderSequence = orderRepository.getSequence();
            for (Order o : orderRepository.findAll()) {
                OrderDto dto = new OrderDto();
                dto.orderId = o.getOrderId();
                dto.customerName = o.getCustomerName();
                dto.sampleId = o.getSample().getId();
                dto.quantity = o.getQuantity();
                dto.status = o.getStatus().name();
                dto.createdAt = o.getCreatedAt().toString();
                snapshot.orders.add(dto);
            }

            for (ProductionJob job : productionQueue.getAll()) {
                QueueItemDto dto = new QueueItemDto();
                dto.orderId = job.getOrder().getOrderId();
                dto.shortfall = job.getShortfall();
                dto.startedAt = job.getStartedAt().toString();
                dto.unitsAdded = job.getUnitsAdded();
                snapshot.productionQueue.add(dto);
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(dbPath.toFile(), snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException("DB 저장 실패: " + dbPath, e);
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class DbSnapshot {
        public List<SampleDto> samples = new ArrayList<>();
        public List<OrderDto> orders = new ArrayList<>();
        public List<QueueItemDto> productionQueue = new ArrayList<>();
        public int orderSequence = 0;
    }

    public static class SampleDto {
        public String id, name;
        public double avgProductionTime;
        public double yield;
        public int stock;
    }

    public static class OrderDto {
        public String orderId, customerName, sampleId, status, createdAt;
        public int quantity;
    }

    public static class QueueItemDto {
        public String orderId;
        public int shortfall;
        public String startedAt;
        public int unitsAdded;
    }
}
