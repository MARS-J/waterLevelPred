package com.waterlevel.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record PredictRequest(
        @JsonProperty("csv_path")
        String csvPath,
        List<Map<String, Object>> records,
        @JsonProperty("pred_len")
        Integer predLen
) {
}
