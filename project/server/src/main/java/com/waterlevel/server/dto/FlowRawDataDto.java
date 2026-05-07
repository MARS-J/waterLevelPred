package com.waterlevel.server.dto;

import java.time.LocalDateTime;

public record FlowRawDataDto(
        Long id,
        LocalDateTime monitorTime,
        Double observedWl,
        Double predictedWl,
        Double windSpeed,
        Double airPressure,
        Double airTemperature,
        Double waterTemperature,
        String stationCode,
        String dataSource,
        String qualityStatus,
        LocalDateTime createdAt
) {
}
