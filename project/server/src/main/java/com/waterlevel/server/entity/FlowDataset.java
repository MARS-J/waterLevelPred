package com.waterlevel.server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "flow_dataset")
public class FlowDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "time_range_start")
    private LocalDateTime timeRangeStart;

    @Column(name = "time_range_end")
    private LocalDateTime timeRangeEnd;

    @Column(name = "record_count")
    private Integer recordCount = 0;

    @Column(name = "quality_score")
    private Double qualityScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FlowDataset() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimeRangeStart() {
        return timeRangeStart;
    }

    public void setTimeRangeStart(LocalDateTime timeRangeStart) {
        this.timeRangeStart = timeRangeStart;
    }

    public LocalDateTime getTimeRangeEnd() {
        return timeRangeEnd;
    }

    public void setTimeRangeEnd(LocalDateTime timeRangeEnd) {
        this.timeRangeEnd = timeRangeEnd;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowDataset that = (FlowDataset) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
