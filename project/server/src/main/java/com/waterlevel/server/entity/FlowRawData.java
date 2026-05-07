package com.waterlevel.server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "flow_raw_data")
public class FlowRawData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_time", nullable = false)
    private LocalDateTime monitorTime;

    @Column(name = "observed_wl")
    private Double observedWl;

    @Column(name = "predicted_wl")
    private Double predictedWl;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "air_pressure")
    private Double airPressure;

    @Column(name = "air_temperature")
    private Double airTemperature;

    @Column(name = "water_temperature")
    private Double waterTemperature;

    @Column(name = "station_code", length = 100)
    private String stationCode;

    @Column(name = "data_source", length = 50)
    private String dataSource;

    @Column(name = "quality_status", length = 20)
    private String qualityStatus = "待校验";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FlowRawData() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getMonitorTime() {
        return monitorTime;
    }

    public void setMonitorTime(LocalDateTime monitorTime) {
        this.monitorTime = monitorTime;
    }

    public Double getObservedWl() {
        return observedWl;
    }

    public void setObservedWl(Double observedWl) {
        this.observedWl = observedWl;
    }

    public Double getPredictedWl() {
        return predictedWl;
    }

    public void setPredictedWl(Double predictedWl) {
        this.predictedWl = predictedWl;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Double getAirPressure() {
        return airPressure;
    }

    public void setAirPressure(Double airPressure) {
        this.airPressure = airPressure;
    }

    public Double getAirTemperature() {
        return airTemperature;
    }

    public void setAirTemperature(Double airTemperature) {
        this.airTemperature = airTemperature;
    }

    public Double getWaterTemperature() {
        return waterTemperature;
    }

    public void setWaterTemperature(Double waterTemperature) {
        this.waterTemperature = waterTemperature;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
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
        FlowRawData that = (FlowRawData) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
