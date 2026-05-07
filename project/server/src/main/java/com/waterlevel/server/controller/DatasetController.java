package com.waterlevel.server.controller;

import com.waterlevel.server.common.ApiResponse;
import com.waterlevel.server.dto.DataMapper;
import com.waterlevel.server.dto.FlowDatasetDto;
import com.waterlevel.server.entity.FlowDataset;
import com.waterlevel.server.service.DatasetService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping("/create")
    public ApiResponse<FlowDatasetDto> createDataset(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        if (name == null || name.isBlank()) {
            return ApiResponse.failure("数据集名称不能为空");
        }

        String description = (String) request.get("description");
        String startTimeStr = (String) request.get("startTime");
        String endTimeStr = (String) request.get("endTime");

        if (startTimeStr == null || endTimeStr == null) {
            return ApiResponse.failure("时间范围不能为空");
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(startTimeStr);
            endTime = LocalDateTime.parse(endTimeStr);
        } catch (DateTimeParseException e) {
            return ApiResponse.failure("时间格式错误: " + e.getMessage());
        }

        if (endTime.isBefore(startTime)) {
            return ApiResponse.failure("结束时间不能早于开始时间");
        }

        FlowDataset dataset = datasetService.createDataset(name, description, startTime, endTime);
        return ApiResponse.success("数据集创建成功", DataMapper.toDto(dataset));
    }

    @GetMapping("/list")
    public ApiResponse<List<FlowDatasetDto>> listDatasets() {
        List<FlowDataset> datasets = datasetService.listDatasets();
        return ApiResponse.success(datasets.stream().map(DataMapper::toDto).toList());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDataset(@PathVariable Long id) {
        datasetService.deleteDataset(id);
        return ApiResponse.success("删除成功", null);
    }
}
