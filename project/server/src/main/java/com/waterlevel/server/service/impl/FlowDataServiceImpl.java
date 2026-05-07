package com.waterlevel.server.service.impl;

import com.waterlevel.server.entity.FlowRawData;
import com.waterlevel.server.repository.FlowRawDataRepository;
import com.waterlevel.server.service.FlowDataService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FlowDataServiceImpl implements FlowDataService {

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private final FlowRawDataRepository rawDataRepository;

    public FlowDataServiceImpl(FlowRawDataRepository rawDataRepository) {
        this.rawDataRepository = rawDataRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> importCsv(MultipartFile file, String defaultDataSource, String defaultStationCode) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return Map.of("successCount", 0, "failCount", 0, "errors", List.of("CSV文件为空"));
            }

            Map<String, Integer> colIndex = parseHeader(headerLine);
            String line;
            int lineNum = 1;
            List<FlowRawData> batch = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                try {
                    FlowRawData data = parseLine(line, colIndex, defaultDataSource, defaultStationCode);
                    if (data.getMonitorTime() != null) {
                        batch.add(data);
                    }
                    if (batch.size() >= 500) {
                        rawDataRepository.saveAll(batch);
                        successCount += batch.size();
                        batch.clear();
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + lineNum + "行: " + e.getMessage());
                    if (errors.size() > 20) {
                        errors.add("... 错误过多，已截断");
                        break;
                    }
                }
            }

            if (!batch.isEmpty()) {
                rawDataRepository.saveAll(batch);
                successCount += batch.size();
            }

        } catch (Exception e) {
            errors.add("文件读取失败: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }

    @Override
    public Page<FlowRawData> listData(LocalDateTime startTime, LocalDateTime endTime,
                                       String dataSource, String stationCode, String qualityStatus,
                                       Pageable pageable) {
        return rawDataRepository.findAll(
                FlowRawDataRepository.withConditions(startTime, endTime, dataSource, stationCode, qualityStatus),
                pageable
        );
    }

    @Override
    public FlowRawData getDetail(Long id) {
        return rawDataRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数据记录不存在: " + id));
    }

    @Override
    @Transactional
    public void deleteData(Long id) {
        rawDataRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        rawDataRepository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public FlowRawData updateData(Long id, FlowRawData data) {
        FlowRawData existing = rawDataRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数据记录不存在: " + id));

        if (data.getMonitorTime() != null) existing.setMonitorTime(data.getMonitorTime());
        if (data.getObservedWl() != null) existing.setObservedWl(data.getObservedWl());
        if (data.getPredictedWl() != null) existing.setPredictedWl(data.getPredictedWl());
        if (data.getWindSpeed() != null) existing.setWindSpeed(data.getWindSpeed());
        if (data.getAirPressure() != null) existing.setAirPressure(data.getAirPressure());
        if (data.getAirTemperature() != null) existing.setAirTemperature(data.getAirTemperature());
        if (data.getWaterTemperature() != null) existing.setWaterTemperature(data.getWaterTemperature());
        if (data.getStationCode() != null) existing.setStationCode(data.getStationCode());
        if (data.getDataSource() != null) existing.setDataSource(data.getDataSource());
        if (data.getQualityStatus() != null) existing.setQualityStatus(data.getQualityStatus());

        return rawDataRepository.save(existing);
    }

    @Override
    public Map<String, Object> checkQuality() {
        List<Object[]> statusCounts = rawDataRepository.countByQualityStatus();
        long total = 0;
        long normal = 0;
        long warning = 0;
        long error = 0;

        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            total += count;
            switch (status) {
                case "正常" -> normal += count;
                case "待校验" -> warning += count;
                case "异常" -> error += count;
            }
        }

        double normalRate = total > 0 ? (double) normal / total * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("normal", normal);
        result.put("warning", warning);
        result.put("error", error);
        result.put("normalRate", Math.round(normalRate * 10.0) / 10.0);
        result.put("suitableForTraining", normalRate >= 80);
        return result;
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> map = new HashMap<>();
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim().toLowerCase().replace(" ", "_"), i);
        }
        return map;
    }

    private FlowRawData parseLine(String line, Map<String, Integer> colIndex,
                                   String defaultDataSource, String defaultStationCode) {
        String[] values = line.split(",", -1);
        FlowRawData data = new FlowRawData();

        // Time field - try multiple possible header names
        String timeStr = getValue(values, colIndex, "date_time", "monitor_time", "time", "timestamp");
        if (timeStr != null && !timeStr.isBlank()) {
            data.setMonitorTime(parseTime(timeStr.trim()));
        }

        // Numeric fields
        data.setObservedWl(parseDouble(getValue(values, colIndex, "observed_wl", "flow_value", "flow", "value")));
        data.setPredictedWl(parseDouble(getValue(values, colIndex, "predicted_wl")));
        data.setWindSpeed(parseDouble(getValue(values, colIndex, "wind_speed")));
        data.setAirPressure(parseDouble(getValue(values, colIndex, "air_press", "air_pressure")));
        data.setAirTemperature(parseDouble(getValue(values, colIndex, "air_temp", "air_temperature")));
        data.setWaterTemperature(parseDouble(getValue(values, colIndex, "water_temp", "water_temperature")));

        // String fields
        data.setStationCode(getValue(values, colIndex, "station_code", "point_name", "station", "测点")
                != null ? getValue(values, colIndex, "station_code", "point_name", "station", "测点") : defaultStationCode);
        data.setDataSource(getValue(values, colIndex, "data_source", "source", "来源")
                != null ? getValue(values, colIndex, "data_source", "source", "来源") : defaultDataSource);
        data.setQualityStatus(getValue(values, colIndex, "quality_status", "quality")
                != null ? getValue(values, colIndex, "quality_status", "quality") : "待校验");

        return data;
    }

    private String getValue(String[] values, Map<String, Integer> colIndex, String... keys) {
        for (String key : keys) {
            Integer idx = colIndex.get(key);
            if (idx != null && idx < values.length) {
                return values[idx].trim();
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String value) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        // Try ISO format with possible 'T'
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析时间格式: " + value);
        }
    }
}
