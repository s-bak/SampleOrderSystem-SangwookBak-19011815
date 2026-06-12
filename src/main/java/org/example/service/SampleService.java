package org.example.service;

import org.example.domain.Sample;
import org.example.repository.SampleRepository;

import java.util.List;

public class SampleService {

    private final SampleRepository sampleRepository;

    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public Sample register(String id, String name, int avgProductionTime, double yield, int initialStock) {
        if (sampleRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id);
        }
        Sample sample = new Sample(id, name, avgProductionTime, yield, initialStock);
        sampleRepository.save(sample);
        return sample;
    }

    public List<Sample> findAll() {
        return sampleRepository.findAll();
    }

    public List<Sample> search(String keyword) {
        return sampleRepository.findByNameContaining(keyword);
    }

    public Sample findById(String id) {
        return sampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료 ID입니다: " + id));
    }
}
