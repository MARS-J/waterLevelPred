package com.waterlevel.server.dto;

import java.time.LocalDateTime;

public record FlowDatasetDto(
        Long id,
        String name,
        String description,
        LocalDateTime timeRangeStart,
        LocalDateTime timeRangeEnd,
        Integer recordCount,
        Double qualityScore,
        LocalDateTime createdAt
) {
}
