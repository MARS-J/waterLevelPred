package com.waterlevel.server.service.impl;

import com.waterlevel.server.entity.FlowDataset;
import com.waterlevel.server.entity.FlowRawData;
import com.waterlevel.server.repository.FlowDatasetRepository;
import com.waterlevel.server.repository.FlowRawDataRepository;
import com.waterlevel.server.service.DatasetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DatasetServiceImpl implements DatasetService {

    private final FlowDatasetRepository datasetRepository;
    private final FlowRawDataRepository rawDataRepository;

    public DatasetServiceImpl(FlowDatasetRepository datasetRepository, FlowRawDataRepository rawDataRepository) {
        this.datasetRepository = datasetRepository;
        this.rawDataRepository = rawDataRepository;
    }

    @Override
    @Transactional
    public FlowDataset createDataset(String name, String description, LocalDateTime startTime, LocalDateTime endTime) {
        List<FlowRawData> records = rawDataRepository.findByMonitorTimeBetween(startTime, endTime);
        FlowDataset dataset = new FlowDataset();
        dataset.setName(name);
        dataset.setDescription(description);
        dataset.setTimeRangeStart(startTime);
        dataset.setTimeRangeEnd(endTime);
        dataset.setRecordCount(records.size());

        double qualityScore = calculateQualityScore(records);
        dataset.setQualityScore(qualityScore);

        return datasetRepository.save(dataset);
    }

    @Override
    public List<FlowDataset> listDatasets() {
        return datasetRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteDataset(Long id) {
        datasetRepository.deleteById(id);
    }

    private double calculateQualityScore(List<FlowRawData> records) {
        if (records.isEmpty()) {
            return 0.0;
        }
        long normalCount = records.stream()
                .filter(r -> "正常".equals(r.getQualityStatus()))
                .count();
        return (double) normalCount / records.size() * 100.0;
    }
}
