package com.waterlevel.server.repository;

import com.waterlevel.server.entity.FlowDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowDatasetRepository extends JpaRepository<FlowDataset, Long> {
}
