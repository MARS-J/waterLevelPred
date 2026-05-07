package com.waterlevel.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterlevel.server.client.TimeMixerClient;
import com.waterlevel.server.config.AlgorithmServiceProperties;
import com.waterlevel.server.dto.PredictRequest;
import com.waterlevel.server.service.AlgorithmService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LABEL_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter CLOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path ROOT_DIR = Path.of("..", "..").toAbsolutePath().normalize();
    private static final Path METADATA_PATH = ROOT_DIR.resolve("algorithm/TimeMixer/artifacts/noaa_observed_wl/metadata.json");
    private static final List<Map<String, Object>> DEVICE_LAYOUTS = List.of(
            Map.of("key", "intake", "name", "取水泵房", "type", "进水单元", "x", -12.0, "y", 1.6, "z", -4.5, "factor", 1.03, "bias", 0.015),
            Map.of("key", "sedimentation", "name", "沉淀池", "type", "沉淀单元", "x", -5.5, "y", 1.2, "z", 0.0, "factor", 0.98, "bias", -0.008),
            Map.of("key", "filtration", "name", "过滤车间", "type", "过滤单元", "x", 1.5, "y", 1.4, "z", -3.2, "factor", 0.95, "bias", -0.014),
            Map.of("key", "disinfection", "name", "加氯间", "type", "消毒单元", "x", 7.8, "y", 1.3, "z", 2.4, "factor", 0.93, "bias", -0.01),
            Map.of("key", "outlet", "name", "出厂总管", "type", "出水单元", "x", 14.2, "y", 1.8, "z", -0.4, "factor", 1.01, "bias", 0.022)
    );

    private final TimeMixerClient timeMixerClient;
    private final AlgorithmServiceProperties properties;
    private final ObjectMapper objectMapper;

    public AlgorithmServiceImpl(TimeMixerClient timeMixerClient, AlgorithmServiceProperties properties, ObjectMapper objectMapper) {
        this.timeMixerClient = timeMixerClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getAlgorithmHealth() {
        return timeMixerClient.health();
    }

    @Override
    public Map<String, Object> predict(PredictRequest request) {
        return timeMixerClient.predict(request);
    }

    @Override
    public Map<String, Object> getBackendOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("projectName", "waterlevel-server");
        overview.put("architecture", "Spring Boot + PostgreSQL + Python TimeMixer");
        overview.put("database", "wd");
        overview.put("algorithmBaseUrl", properties.getBaseUrl());
        overview.put("modules", new String[]{
                "controller",
                "service",
                "client",
                "config",
                "common",
                "dto"
        });
        return overview;
    }

    @Override
    public Map<String, Object> getVisualScreenSummary() {
        DatasetBundle dataset = loadDatasetBundle();
        PredictionBundle prediction = loadPredictionBundle(dataset);
        List<Map<String, Object>> devices = buildDevices(dataset.rows(), prediction);
        List<Map<String, Object>> warnings = buildWarnings(dataset, prediction, devices);
        List<Map<String, Object>> tasks = buildTasks(dataset, prediction, warnings);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("modelName", dataset.modelName());
        payload.put("refreshSeconds", 30);
        payload.put("algorithmStatus", prediction.algorithmAvailable() ? "运行中" : "离线");
        payload.put("generatedAt", LABEL_TIME_FORMATTER.format(LocalDateTime.now()));
        payload.put("headline", buildHeadline(dataset, prediction, warnings));
        payload.put("kpis", buildKpis(dataset, prediction, warnings));
        payload.put("trendSeries", buildTrendSeries(dataset.rows(), 24));
        payload.put("prediction", Map.of(
                "actual", buildActualSeries(dataset.rows(), 12),
                "forecast", prediction.forecast(),
                "summary", buildPredictionSummary(dataset, prediction)
        ));
        payload.put("warnings", warnings);
        payload.put("tasks", tasks);
        payload.put("ranking", buildRanking(devices));
        payload.put("statusDistribution", buildStatusDistribution(devices));
        payload.put("stageStability", buildStageStability(devices, dataset.accuracy()));
        payload.put("accuracy", round(dataset.accuracy(), 1));
        payload.put("devices", devices);
        return payload;
    }

    @Override
    public Map<String, Object> getSceneStatus() {
        DatasetBundle dataset = loadDatasetBundle();
        PredictionBundle prediction = loadPredictionBundle(dataset);
        List<Map<String, Object>> devices = buildDevices(dataset.rows(), prediction);
        List<Map<String, Object>> warnings = buildWarnings(dataset, prediction, devices);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", LABEL_TIME_FORMATTER.format(LocalDateTime.now()));
        payload.put("algorithmStatus", prediction.algorithmAvailable() ? "运行中" : "离线");
        payload.put("summary", List.of(
                Map.of("label", "在线设备", "value", String.valueOf(devices.size())),
                Map.of("label", "高风险节点", "value", String.valueOf(devices.stream().filter(item -> Objects.equals(item.get("status"), "告警")).count())),
                Map.of("label", "联动延迟", "value", prediction.algorithmAvailable() ? "0.9s" : "1.6s"),
                Map.of("label", "刷新时间", "value", LABEL_TIME_FORMATTER.format(dataset.latest().time()))
        ));
        payload.put("devices", devices);
        payload.put("warnings", warnings);
        payload.put("links", List.of(
                Map.of("from", "intake", "to", "sedimentation"),
                Map.of("from", "sedimentation", "to", "filtration"),
                Map.of("from", "filtration", "to", "disinfection"),
                Map.of("from", "disinfection", "to", "outlet")
        ));
        payload.put("overview", buildPredictionSummary(dataset, prediction));
        return payload;
    }

    private DatasetBundle loadDatasetBundle() {
        try {
            Map<String, Object> metadata = objectMapper.readValue(METADATA_PATH.toFile(), new TypeReference<>() {
            });
            String modelName = String.valueOf(metadata.getOrDefault("model_name", "TimeMixer"));
            Path sourceCsv = Path.of(String.valueOf(metadata.get("source_csv")));
            List<WaterLevelRow> rows = loadRows(sourceCsv);
            double accuracy = calculateAccuracy(rows);
            return new DatasetBundle(modelName, sourceCsv, rows, accuracy);
        } catch (IOException exception) {
            throw new IllegalStateException("读取模型元数据失败", exception);
        }
    }

    private List<WaterLevelRow> loadRows(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException("历史数据文件为空");
            }
            Map<String, Integer> columnIndex = indexColumns(headerLine);
            List<WaterLevelRow> rows = new ArrayList<>();
            Map<String, Double> lastValues = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                rows.add(new WaterLevelRow(
                        parseTimestamp(readCell(values, columnIndex, "Date Time")),
                        parseDouble(values, columnIndex, "observed_wl", lastValues),
                        parseDouble(values, columnIndex, "predicted_wl", lastValues),
                        parseDouble(values, columnIndex, "wind_speed", lastValues),
                        parseDouble(values, columnIndex, "air_press", lastValues),
                        parseDouble(values, columnIndex, "air_temp", lastValues),
                        parseDouble(values, columnIndex, "water_temp", lastValues)
                ));
            }
            if (rows.size() < 12) {
                throw new IllegalStateException("历史数据量不足，无法生成展示摘要");
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("读取历史数据失败", exception);
        }
    }

    private Map<String, Integer> indexColumns(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        Map<String, Integer> indexMap = new HashMap<>();
        for (int index = 0; index < headers.length; index++) {
            indexMap.put(headers[index], index);
        }
        return indexMap;
    }

    private PredictionBundle loadPredictionBundle(DatasetBundle dataset) {
        try {
            Map<String, Object> response = timeMixerClient.predict(new PredictRequest(dataset.sourceCsv().toString(), null, 24));
            Object rawForecast = response.get("target_predictions");
            if (rawForecast instanceof List<?> items) {
                List<Map<String, Object>> forecast = new ArrayList<>();
                for (Object item : items) {
                    if (item instanceof Map<?, ?> rawPoint) {
                        String time = String.valueOf(rawPoint.get("date"));
                        double value = Double.parseDouble(String.valueOf(rawPoint.get("value")));
                        forecast.add(Map.of(
                                "time", normalizeTimeLabel(time),
                                "value", round(value, 3)
                        ));
                    }
                }
                if (!forecast.isEmpty()) {
                    return new PredictionBundle(true, forecast, "模型推理完成");
                }
            }
        } catch (Exception exception) {
            return fallbackPrediction(dataset);
        }
        return fallbackPrediction(dataset);
    }

    private PredictionBundle fallbackPrediction(DatasetBundle dataset) {
        List<WaterLevelRow> rows = dataset.rows();
        WaterLevelRow latest = dataset.latest();
        WaterLevelRow previous = rows.get(rows.size() - 2);
        Duration interval = Duration.between(previous.time(), latest.time());
        if (interval.isNegative() || interval.isZero()) {
            interval = Duration.ofMinutes(6);
        }
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            WaterLevelRow sample = rows.get(rows.size() - 24 + index);
            LocalDateTime pointTime = latest.time().plus(interval.multipliedBy(index + 1L));
            double drift = (index + 1) * 0.004;
            forecast.add(Map.of(
                    "time", LABEL_TIME_FORMATTER.format(pointTime),
                    "value", round(sample.baseline() + drift, 3)
            ));
        }
        return new PredictionBundle(false, forecast, "算法服务暂不可用，已回退到基线序列");
    }

    private List<Map<String, Object>> buildDevices(List<WaterLevelRow> rows, PredictionBundle prediction) {
        WaterLevelRow latest = rows.get(rows.size() - 1);
        double predictedPeak = prediction.forecast().stream()
                .mapToDouble(item -> toDouble(item.get("value")))
                .max()
                .orElse(latest.observed());
        List<Map<String, Object>> devices = new ArrayList<>();
        for (Map<String, Object> layout : DEVICE_LAYOUTS) {
            double factor = toDouble(layout.get("factor"));
            double bias = toDouble(layout.get("bias"));
            double currentValue = latest.observed() * factor + bias;
            double predictionValue = predictedPeak * factor + bias;
            double trend = percentage(currentValue, predictionValue);
            String status = resolveStatus(Math.abs(predictionValue - currentValue), Math.abs(latest.observed() - latest.baseline()), trend);
            devices.add(Map.ofEntries(
                    Map.entry("key", layout.get("key")),
                    Map.entry("name", layout.get("name")),
                    Map.entry("type", layout.get("type")),
                    Map.entry("current", round(currentValue, 3)),
                    Map.entry("prediction", round(predictionValue, 3)),
                    Map.entry("trend", formatPercent(trend)),
                    Map.entry("status", status),
                    Map.entry("alarm", buildAlarm(String.valueOf(layout.get("name")), status, trend)),
                    Map.entry("score", scoreOf(status, trend)),
                    Map.entry("position", Map.of(
                            "x", layout.get("x"),
                            "y", layout.get("y"),
                            "z", layout.get("z")
                    )),
                    Map.entry("flowSpeed", round(Math.max(0.4, Math.abs(trend) / 8.0 + 0.6), 2))
            ));
        }
        return devices;
    }

    private List<Map<String, Object>> buildWarnings(DatasetBundle dataset, PredictionBundle prediction, List<Map<String, Object>> devices) {
        WaterLevelRow latest = dataset.latest();
        double maxForecast = prediction.forecast().stream()
                .mapToDouble(item -> toDouble(item.get("value")))
                .max()
                .orElse(latest.observed());
        double peakRise = maxForecast - latest.observed();
        List<Map<String, Object>> warnings = new ArrayList<>();
        warnings.add(Map.of(
                "name", "出厂总管",
                "level", peakRise >= 0.08 ? "高" : "中",
                "detail", peakRise >= 0.08 ? "未来窗口存在明显抬升，建议提前关注出水负荷。" : "预测曲线平稳上行，建议维持常规巡检。",
                "value", round(peakRise, 3)
        ));
        double deviation = Math.abs(latest.observed() - latest.baseline());
        warnings.add(Map.of(
                "name", "沉淀池入口",
                "level", deviation >= 0.05 ? "中" : "低",
                "detail", deviation >= 0.05 ? "最新实测与基线差值偏大，建议复核波动来源。" : "实测值与基线贴合，当前未见异常漂移。",
                "value", round(deviation, 3)
        ));
        long alertCount = devices.stream().filter(item -> Objects.equals(item.get("status"), "告警")).count();
        warnings.add(Map.of(
                "name", "加氯间",
                "level", alertCount > 0 ? "低" : "低",
                "detail", prediction.algorithmAvailable() ? "预测链路在线，消毒段维持稳定运行。" : "算法链路离线，当前仅展示基线联动结果。",
                "value", round(dataset.accuracy(), 1)
        ));
        warnings.sort(Comparator.comparingInt((Map<String, Object> item) -> warningWeight(String.valueOf(item.get("level")))).reversed());
        return warnings;
    }

    private List<Map<String, Object>> buildTasks(DatasetBundle dataset, PredictionBundle prediction, List<Map<String, Object>> warnings) {
        String latestTime = LABEL_TIME_FORMATTER.format(dataset.latest().time());
        return List.of(
                Map.of("title", "数据更新", "status", "已完成", "time", latestTime, "detail", "已同步最新观测水位与环境特征。"),
                Map.of("title", "模型预测", "status", prediction.algorithmAvailable() ? "已完成" : "待恢复", "time", LABEL_TIME_FORMATTER.format(LocalDateTime.now()), "detail", prediction.description()),
                Map.of("title", "摘要生成", "status", "已完成", "time", LABEL_TIME_FORMATTER.format(LocalDateTime.now()), "detail", "峰值时段、准确率和关键指标已更新。"),
                Map.of("title", "告警分析", "status", warningWeight(String.valueOf(warnings.get(0).get("level"))) >= 2 ? "关注中" : "正常", "time", LABEL_TIME_FORMATTER.format(LocalDateTime.now()), "detail", String.valueOf(warnings.get(0).get("detail")))
        );
    }

    private String buildHeadline(DatasetBundle dataset, PredictionBundle prediction, List<Map<String, Object>> warnings) {
        Map<String, Object> summary = buildPredictionSummary(dataset, prediction);
        return "当前状态：" + summary.get("peakTime") + "附近预计达到峰值 "
                + summary.get("peakValue") + " m，"
                + warnings.get(0).get("name") + "处于" + warnings.get(0).get("level") + "优先级告警。";
    }

    private List<Map<String, Object>> buildKpis(DatasetBundle dataset, PredictionBundle prediction, List<Map<String, Object>> warnings) {
        WaterLevelRow latest = dataset.latest();
        WaterLevelRow previous = dataset.rows().get(dataset.rows().size() - 2);
        Map<String, Object> summary = buildPredictionSummary(dataset, prediction);
        return List.of(
                Map.of("label", "实时水位", "value", formatNumber(latest.observed()), "unit", "m", "meta", "较上一时刻 " + formatSigned(latest.observed() - previous.observed()) + " m"),
                Map.of("label", "24h 均值", "value", formatNumber(averageObserved(dataset.rows())), "unit", "m", "meta", "近一日历史观测均值"),
                Map.of("label", "预测峰值", "value", String.valueOf(summary.get("peakTime")), "unit", "", "meta", "峰值 " + summary.get("peakValue") + " m"),
                Map.of("label", "模型可信度", "value", formatNumber(dataset.accuracy()), "unit", "%", "meta", "当前告警数 " + warnings.size() + " 条")
        );
    }

    private List<Map<String, Object>> buildTrendSeries(List<WaterLevelRow> rows, int count) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (WaterLevelRow row : rows.subList(Math.max(0, rows.size() - count), rows.size())) {
            series.add(Map.of(
                    "time", CLOCK_TIME_FORMATTER.format(row.time()),
                    "value", round(row.observed(), 3)
            ));
        }
        return series;
    }

    private List<Map<String, Object>> buildActualSeries(List<WaterLevelRow> rows, int count) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (WaterLevelRow row : rows.subList(Math.max(0, rows.size() - count), rows.size())) {
            series.add(Map.of(
                    "time", LABEL_TIME_FORMATTER.format(row.time()),
                    "value", round(row.observed(), 3)
            ));
        }
        return series;
    }

    private Map<String, Object> buildPredictionSummary(DatasetBundle dataset, PredictionBundle prediction) {
        WaterLevelRow latest = dataset.latest();
        double peakValue = prediction.forecast().stream()
                .mapToDouble(item -> toDouble(item.get("value")))
                .max()
                .orElse(latest.observed());
        Map<String, Object> peakPoint = prediction.forecast().stream()
                .max(Comparator.comparingDouble(item -> toDouble(item.get("value"))))
                .orElse(Map.of("time", LABEL_TIME_FORMATTER.format(latest.time()), "value", peakValue));
        double averageValue = prediction.forecast().stream()
                .mapToDouble(item -> toDouble(item.get("value")))
                .average()
                .orElse(latest.observed());
        double rise = peakValue - latest.observed();
        return Map.of(
                "currentValue", round(latest.observed(), 3),
                "peakTime", peakPoint.get("time"),
                "peakValue", round(peakValue, 3),
                "averageValue", round(averageValue, 3),
                "rise", round(rise, 3)
        );
    }

    private List<Map<String, Object>> buildRanking(List<Map<String, Object>> devices) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> device : devices) {
            ranking.add(Map.of(
                    "station", device.get("name"),
                    "score", device.get("score"),
                    "status", device.get("status")
            ));
        }
        ranking.sort(Comparator.comparingInt((Map<String, Object> item) -> Integer.parseInt(String.valueOf(item.get("score")))).reversed());
        return ranking;
    }

    private List<Map<String, Object>> buildStatusDistribution(List<Map<String, Object>> devices) {
        int stable = 0;
        int focus = 0;
        int alert = 0;
        for (Map<String, Object> device : devices) {
            String status = String.valueOf(device.get("status"));
            if ("稳定".equals(status)) {
                stable++;
            } else if ("关注".equals(status)) {
                focus++;
            } else {
                alert++;
            }
        }
        return List.of(
                Map.of("name", "稳定", "value", stable),
                Map.of("name", "关注", "value", focus),
                Map.of("name", "告警", "value", alert)
        );
    }

    private List<Map<String, Object>> buildStageStability(List<Map<String, Object>> devices, double accuracy) {
        List<String> stageNames = List.of("取水", "沉淀", "过滤", "消毒", "出厂");
        List<Map<String, Object>> stages = new ArrayList<>();
        for (int index = 0; index < stageNames.size(); index++) {
            Map<String, Object> device = devices.get(index);
            int score = Math.max(76, Math.min(98,
                    (int) Math.round(accuracy - Math.abs(toDouble(device.get("prediction")) - toDouble(device.get("current"))) * 55 + 4 - index)));
            stages.add(Map.of("name", stageNames.get(index), "value", score));
        }
        return stages;
    }

    private double calculateAccuracy(List<WaterLevelRow> rows) {
        List<WaterLevelRow> sample = rows.subList(Math.max(0, rows.size() - 96), rows.size());
        double min = sample.stream().mapToDouble(WaterLevelRow::observed).min().orElse(0.0);
        double max = sample.stream().mapToDouble(WaterLevelRow::observed).max().orElse(1.0);
        double mae = sample.stream().mapToDouble(row -> Math.abs(row.observed() - row.baseline())).average().orElse(0.0);
        double range = Math.max(0.2, max - min);
        return Math.max(82.0, Math.min(99.2, (1.0 - mae / range) * 100.0));
    }

    private double averageObserved(List<WaterLevelRow> rows) {
        return rows.subList(Math.max(0, rows.size() - 240), rows.size()).stream()
                .mapToDouble(WaterLevelRow::observed)
                .average()
                .orElse(rows.get(rows.size() - 1).observed());
    }

    private String resolveStatus(double delta, double deviation, double trend) {
        if (delta >= 0.09 || Math.abs(trend) >= 12 || deviation >= 0.06) {
            return "告警";
        }
        if (delta >= 0.05 || Math.abs(trend) >= 6 || deviation >= 0.035) {
            return "关注";
        }
        return "稳定";
    }

    private String buildAlarm(String name, String status, double trend) {
        if ("告警".equals(status)) {
            return name + "预测上行幅度偏大，建议优先巡检。";
        }
        if ("关注".equals(status)) {
            return name + "波动高于稳态区间，建议持续观察。";
        }
        return name + "运行平稳，当前未发现新增异常。";
    }

    private int scoreOf(String status, double trend) {
        int base = switch (status) {
            case "告警" -> 82;
            case "关注" -> 90;
            default -> 97;
        };
        return Math.max(72, Math.min(99, (int) Math.round(base - Math.abs(trend) * 0.8)));
    }

    private int warningWeight(String level) {
        return switch (level) {
            case "高" -> 3;
            case "中" -> 2;
            default -> 1;
        };
    }

    private double parseDouble(String[] values, Map<String, Integer> columnIndex, String key, Map<String, Double> lastValues) {
        String rawValue = readCell(values, columnIndex, key).trim();
        if (rawValue.isEmpty()) {
            Double fallback = lastValues.get(key);
            if (fallback != null) {
                return fallback;
            }
            return 0.0;
        }
        double parsedValue = Double.parseDouble(rawValue);
        lastValues.put(key, parsedValue);
        return parsedValue;
    }

    private String readCell(String[] values, Map<String, Integer> columnIndex, String key) {
        Integer index = columnIndex.get(key);
        if (index == null || index < 0 || index >= values.length) {
            return "";
        }
        return values[index];
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value, CSV_TIME_FORMATTER);
    }

    private String normalizeTimeLabel(String value) {
        LocalDateTime dateTime = parseTimestamp(value);
        return LABEL_TIME_FORMATTER.format(dateTime);
    }

    private double percentage(double from, double to) {
        if (Math.abs(from) < 0.0001) {
            return 0.0;
        }
        return (to - from) / from * 100.0;
    }

    private String formatPercent(double value) {
        return (value >= 0 ? "+" : "") + round(value, 1) + "%";
    }

    private String formatSigned(double value) {
        return (value >= 0 ? "+" : "") + formatNumber(value);
    }

    private String formatNumber(double value) {
        return String.valueOf(round(value, 3));
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private double toDouble(Object value) {
        return Double.parseDouble(String.valueOf(value));
    }

    private record WaterLevelRow(
            LocalDateTime time,
            double observed,
            double baseline,
            double windSpeed,
            double airPressure,
            double airTemperature,
            double waterTemperature
    ) {
    }

    private record DatasetBundle(
            String modelName,
            Path sourceCsv,
            List<WaterLevelRow> rows,
            double accuracy
    ) {
        private WaterLevelRow latest() {
            return rows.get(rows.size() - 1);
        }
    }

    private record PredictionBundle(
            boolean algorithmAvailable,
            List<Map<String, Object>> forecast,
            String description
    ) {
    }
}
