package com.waterlevel.server.controller;

import com.waterlevel.server.common.ApiResponse;
import com.waterlevel.server.dto.DataMapper;
import com.waterlevel.server.dto.FlowRawDataDto;
import com.waterlevel.server.entity.FlowRawData;
import com.waterlevel.server.service.FlowDataService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data")
public class DataController {

    private static final int MAX_PAGE_SIZE = 200;

    private final FlowDataService flowDataService;

    public DataController(FlowDataService flowDataService) {
        this.flowDataService = flowDataService;
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dataSource", required = false, defaultValue = "历史导入") String dataSource,
            @RequestParam(value = "stationCode", required = false, defaultValue = "进水总管") String stationCode) {
        if (file.isEmpty()) {
            return ApiResponse.failure("文件不能为空");
        }
        return ApiResponse.success("导入完成", flowDataService.importCsv(file, dataSource, stationCode));
    }

    @GetMapping("/list")
    public ApiResponse<Page<FlowRawDataDto>> listData(
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "dataSource", required = false) String dataSource,
            @RequestParam(value = "stationCode", required = false) String stationCode,
            @RequestParam(value = "qualityStatus", required = false) String qualityStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("monitorTime").descending());
        Page<FlowRawData> result = flowDataService.listData(startTime, endTime, dataSource, stationCode, qualityStatus, pageable);
        return ApiResponse.success(result.map(DataMapper::toDto));
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<FlowRawDataDto> getDetail(@PathVariable Long id) {
        FlowRawData data = flowDataService.getDetail(id);
        return ApiResponse.success(DataMapper.toDto(data));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteData(@PathVariable Long id) {
        flowDataService.deleteData(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.failure("ID列表不能为空");
        }
        flowDataService.batchDelete(ids);
        return ApiResponse.success("批量删除成功", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<FlowRawDataDto> updateData(@PathVariable Long id, @RequestBody FlowRawData data) {
        FlowRawData updated = flowDataService.updateData(id, data);
        return ApiResponse.success(DataMapper.toDto(updated));
    }

    @PostMapping("/quality/check")
    public ApiResponse<Map<String, Object>> checkQuality() {
        return ApiResponse.success(flowDataService.checkQuality());
    }
}
