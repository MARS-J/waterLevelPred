package com.waterlevel.server.repository;

import com.waterlevel.server.entity.FlowRawData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlowRawDataRepository extends JpaRepository<FlowRawData, Long>, JpaSpecificationExecutor<FlowRawData> {

    List<FlowRawData> findByMonitorTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT f.qualityStatus, COUNT(f) FROM FlowRawData f GROUP BY f.qualityStatus")
    List<Object[]> countByQualityStatus();

    @Query("SELECT f.dataSource, COUNT(f) FROM FlowRawData f GROUP BY f.dataSource")
    List<Object[]> countByDataSource();

    @Query("SELECT f.stationCode, COUNT(f) FROM FlowRawData f GROUP BY f.stationCode")
    List<Object[]> countByStationCode();

    static Specification<FlowRawData> withConditions(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String dataSource,
            String stationCode,
            String qualityStatus) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("monitorTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monitorTime"), endTime));
            }
            if (dataSource != null && !dataSource.isBlank()) {
                predicates.add(cb.like(root.get("dataSource"), "%" + dataSource + "%"));
            }
            if (stationCode != null && !stationCode.isBlank()) {
                predicates.add(cb.like(root.get("stationCode"), "%" + stationCode + "%"));
            }
            if (qualityStatus != null && !qualityStatus.isBlank()) {
                predicates.add(cb.equal(root.get("qualityStatus"), qualityStatus));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
