package com.waterlevel.server.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterlevel.server.config.AlgorithmServiceProperties;
import com.waterlevel.server.dto.PredictRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class TimeMixerClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AlgorithmServiceProperties properties;

    public TimeMixerClient(ObjectMapper objectMapper, AlgorithmServiceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    public Map<String, Object> health() {
        return get("/health");
    }

    public Map<String, Object> predict(PredictRequest request) {
        return post("/predict", request);
    }

    private Map<String, Object> get(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + path))
                .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .GET()
                .build();
        return send(request);
    }

    private Map<String, Object> post(String path, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + path))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return send(request);
        } catch (IOException exception) {
            throw new IllegalStateException("序列化算法请求失败", exception);
        }
    }

    private Map<String, Object> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("算法服务调用失败: " + response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("读取算法服务响应失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("算法服务调用被中断", exception);
        }
    }
}
