package com.waterlevel.server.service;

import com.waterlevel.server.entity.FlowDataset;

import java.time.LocalDateTime;
import java.util.List;

public interface DatasetService {

    FlowDataset createDataset(String name, String description,
                              LocalDateTime startTime, LocalDateTime endTime);

    List<FlowDataset> listDatasets();

    void deleteDataset(Long id);
}
