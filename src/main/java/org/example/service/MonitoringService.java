package org.example.service;

import org.example.domain.Order;
import org.example.domain.OrderStatus;
import org.example.domain.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MonitoringService {

    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;

    public MonitoringService(SampleRepository sampleRepository, OrderRepository orderRepository) {
        this.sampleRepository = sampleRepository;
        this.orderRepository = orderRepository;
    }

    public Map<OrderStatus, List<Order>> getOrdersByStatus() {
        Map<OrderStatus, List<Order>> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.RESERVED, OrderStatus.PRODUCING,
                OrderStatus.CONFIRMED, OrderStatus.RELEASE}) {
            result.put(status, orderRepository.findByStatus(status));
        }
        return result;
    }

    public List<StockStatusEntry> getStockStatus() {
        List<StockStatusEntry> entries = new ArrayList<>();
        for (Sample sample : sampleRepository.findAll()) {
            int pendingQuantity = 0;
            int confirmedQuantity = 0;
            for (Order order : orderRepository.findAll()) {
                if (!order.getSample().getId().equals(sample.getId())) continue;
                if (order.getStatus() == OrderStatus.RESERVED
                        || order.getStatus() == OrderStatus.PRODUCING) {
                    pendingQuantity += order.getQuantity();
                } else if (order.getStatus() == OrderStatus.CONFIRMED) {
                    confirmedQuantity += order.getQuantity();
                }
            }
            entries.add(new StockStatusEntry(sample, pendingQuantity, confirmedQuantity));
        }
        return entries;
    }

    public static class StockStatusEntry {

        private final Sample sample;
        private final String status;
        private final int pendingQuantity;

        public StockStatusEntry(Sample sample, int pendingQuantity, int confirmedQuantity) {
            this.sample = sample;
            this.pendingQuantity = pendingQuantity;
            this.status = determineStatus(sample.getStock(), confirmedQuantity, pendingQuantity);
        }

        private static String determineStatus(int stock, int confirmedQuantity, int pendingQuantity) {
            if (stock == 0) return "고갈";
            if (pendingQuantity > 0) return "부족";
            return "여유";
        }

        public Sample getSample() { return sample; }
        public String getStatus() { return status; }
        public int getPendingQuantity() { return pendingQuantity; }
    }
}
