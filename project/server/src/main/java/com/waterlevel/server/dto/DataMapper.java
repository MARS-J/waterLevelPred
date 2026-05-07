package com.waterlevel.server.dto;

import com.waterlevel.server.entity.FlowDataset;
import com.waterlevel.server.entity.FlowRawData;

public final class DataMapper {

    private DataMapper() {
    }

    public static FlowRawDataDto toDto(FlowRawData entity) {
        if (entity == null) {
            return null;
        }
        return new FlowRawDataDto(
                entity.getId(),
                entity.getMonitorTime(),
                entity.getObservedWl(),
                entity.getPredictedWl(),
                entity.getWindSpeed(),
                entity.getAirPressure(),
                entity.getAirTemperature(),
                entity.getWaterTemperature(),
                entity.getStationCode(),
                entity.getDataSource(),
                entity.getQualityStatus(),
                entity.getCreatedAt()
        );
    }

    public static FlowDatasetDto toDto(FlowDataset entity) {
        if (entity == null) {
            return null;
        }
        return new FlowDatasetDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTimeRangeStart(),
                entity.getTimeRangeEnd(),
                entity.getRecordCount(),
                entity.getQualityScore(),
                entity.getCreatedAt()
        );
    }
}
