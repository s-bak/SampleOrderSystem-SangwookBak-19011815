package org.example.repository;

import org.example.domain.Sample;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class SampleRepository {

    private final Map<String, Sample> store = new LinkedHashMap<>();

    public void save(Sample sample) {
        if (store.containsKey(sample.getId())) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID입니다: " + sample.getId());
        }
        store.put(sample.getId(), sample);
    }

    public Optional<Sample> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Sample> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Sample> findByNameContaining(String keyword) {
        return filter(s -> s.getName().contains(keyword));
    }

    public List<Sample> findByIdContaining(String keyword) {
        return filter(s -> s.getId().contains(keyword));
    }

    public List<Sample> findByAvgProductionTime(double time) {
        return filter(s -> Double.compare(s.getAvgProductionTime(), time) == 0);
    }

    public List<Sample> findByYield(double yield) {
        return filter(s -> Double.compare(s.getYield(), yield) == 0);
    }

    private List<Sample> filter(Predicate<Sample> predicate) {
        List<Sample> result = new ArrayList<>();
        for (Sample sample : store.values()) {
            if (predicate.test(sample)) {
                result.add(sample);
            }
        }
        return result;
    }
}
