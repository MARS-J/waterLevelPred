package com.waterlevel.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterlevel.server.config.AiAssistantProperties;
import com.waterlevel.server.dto.AiAssistantChatRequest;
import com.waterlevel.server.dto.AiAssistantChatResponse;
import com.waterlevel.server.service.AiAssistantService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final AiAssistantProperties properties;
    private final OkHttpClient okHttpClient;

    public AiAssistantServiceImpl(ObjectMapper objectMapper, AiAssistantProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .readTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .writeTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        if (properties.getProxyHost() != null && !properties.getProxyHost().isBlank() && properties.getProxyPort() > 0) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(properties.getProxyHost(), properties.getProxyPort())));
        }
        this.okHttpClient = builder.build();
    }

    @Override
    public AiAssistantChatResponse chat(AiAssistantChatRequest request) {
        validateApiKey();
        Map<String, Object> response = send(buildPayload(request, false), false);
        String conversationId = resolveConversationId(request);
        List<Map<String, Object>> choices = asListOfMap(response.get("choices"));
        if (choices.isEmpty()) {
            throw new IllegalStateException("AI 服务未返回有效结果");
        }
        Map<String, Object> message = asMap(choices.getFirst().get("message"));
        Map<String, Object> usage = asMap(response.get("usage"));
        return new AiAssistantChatResponse(
                conversationId,
                String.valueOf(response.getOrDefault("model", properties.getModel())),
                extractReply(message),
                readInteger(usage.get("prompt_tokens")),
                readInteger(usage.get("completion_tokens")),
                readInteger(usage.get("total_tokens"))
        );
    }

    @Override
    public void streamChat(AiAssistantChatRequest request, Consumer<Map<String, Object>> eventConsumer) {
        String conversationId = resolveConversationId(request);
        StringBuilder reply = new StringBuilder();
        String model = properties.getModel();
        Map<String, Object> usage = Map.of();
        try {
            validateApiKey();
            try (Response response = sendStreaming(buildPayload(request, true), true)) {
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("AI 服务调用失败: " + readResponseBody(responseBody));
                }
                if (responseBody == null) {
                    throw new IllegalStateException("AI 服务未返回响应体");
                }
                eventConsumer.accept(Map.of(
                        "type", "started",
                        "conversationId", conversationId,
                        "model", model
                ));
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        String data = line.substring(5).trim();
                        if (data.isEmpty()) {
                            continue;
                        }
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        Map<String, Object> chunk = objectMapper.readValue(data, new TypeReference<>() {
                        });
                        model = String.valueOf(chunk.getOrDefault("model", model));
                        List<Map<String, Object>> choices = asListOfMap(chunk.get("choices"));
                        if (!choices.isEmpty()) {
                            Map<String, Object> choice = choices.getFirst();
                            String delta = extractStreamingText(asMap(choice.get("delta")));
                            if (!delta.isBlank()) {
                                reply.append(delta);
                                eventConsumer.accept(Map.of(
                                        "type", "chunk",
                                        "conversationId", conversationId,
                                        "model", model,
                                        "delta", delta,
                                        "reply", reply.toString()
                                ));
                            }
                        }
                        Map<String, Object> chunkUsage = asMap(chunk.get("usage"));
                        if (!chunkUsage.isEmpty()) {
                            usage = chunkUsage;
                        }
                    }
                }
            }
            eventConsumer.accept(buildDoneEvent(conversationId, model, reply.toString(), usage));
        } catch (Exception exception) {
            if (isInterrupted(exception)) {
                if (!reply.isEmpty()) {
                    eventConsumer.accept(buildDoneEvent(conversationId, model, reply.toString(), usage));
                    return;
                }
                eventConsumer.accept(Map.of(
                        "type", "error",
                        "conversationId", conversationId,
                        "message", "对话已中断，请重试"
                ));
                return;
            }
            eventConsumer.accept(Map.of(
                    "type", "error",
                    "conversationId", conversationId,
                    "message", exception.getMessage() == null ? exception.toString() : exception.getClass().getSimpleName() + ": " + exception.getMessage()
            ));
        }
    }

    private Map<String, Object> buildPayload(AiAssistantChatRequest request, boolean stream) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", """
                        你是水厂流量预测分析平台内置的 AI 智能助手。回答要专业、简洁、可信，优先围绕水厂运行、数据分析、模型训练、预测解释和页面使用方式展开。
                        只输出最终回答，不要输出思考过程、推理过程、Thinking Process、reasoning_content 或任何中间分析。
                        如果用户上传了图片，请结合图片内容和上下文回答；如果图片信息不足，明确指出不确定性，不要编造。
                        """
        ));
        for (AiAssistantChatRequest.Message item : request.messages()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", normalizeRole(item.role()));
            message.put("content", buildMessageContent(item));
            messages.add(message);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", messages);
        payload.put("enable_thinking", false);
        if (stream) {
            payload.put("stream", true);
        }
        return payload;
    }

    private Map<String, Object> send(Map<String, Object> body, boolean stream) {
        try (Response response = okHttpClient.newCall(buildHttpRequest(body, stream)).execute()) {
            String responseText = readResponseBody(response.body());
            if (!response.isSuccessful()) {
                throw new IllegalStateException("AI 服务调用失败: " + responseText);
            }
            try {
                return objectMapper.readValue(responseText, new TypeReference<>() {
                });
            } catch (IOException exception) {
                throw new IllegalStateException("解析 AI 服务响应失败: " + exception.getMessage() + "，响应片段: " + abbreviate(responseText), exception);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("读取 AI 服务响应失败: " + exception.getMessage(), exception);
        }
    }

    private Response sendStreaming(Map<String, Object> body, boolean stream) throws IOException {
        return okHttpClient.newCall(buildHttpRequest(body, stream)).execute();
    }

    private Request buildHttpRequest(Map<String, Object> body, boolean stream) throws IOException {
        return new Request.Builder()
                .url(properties.getBaseUrl() + "/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Accept", stream ? "text/event-stream" : "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA_TYPE))
                .build();
    }

    private String extractReply(Object rawContent) {
        if (rawContent instanceof String text) {
            return text;
        }
        if (rawContent instanceof List<?> items) {
            StringBuilder builder = new StringBuilder();
            for (Object item : items) {
                Map<String, Object> contentItem = asMap(item);
                Object text = contentItem.get("text");
                if (text != null) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        if (rawContent instanceof Map<?, ?> rawMap) {
            Object text = rawMap.get("text");
            if (text != null && !String.valueOf(text).isBlank()) {
                return String.valueOf(text);
            }
        }
        return "";
    }

    private String extractReply(Map<String, Object> message) {
        String content = extractReply(message.get("content"));
        if (!content.isBlank()) {
            return content;
        }
        throw new IllegalStateException("AI 服务返回内容为空");
    }

    private String extractStreamingText(Map<String, Object> delta) {
        String content = extractReply(delta.get("content"));
        if (!content.isBlank()) {
            return content;
        }
        Object text = delta.get("text");
        return text == null ? "" : String.valueOf(text);
    }

    private Map<String, Object> buildDoneEvent(String conversationId, String model, String reply, Map<String, Object> usage) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "done");
        result.put("conversationId", conversationId);
        result.put("model", model);
        result.put("reply", reply);
        result.put("promptTokens", readInteger(usage.get("prompt_tokens")));
        result.put("completionTokens", readInteger(usage.get("completion_tokens")));
        result.put("totalTokens", readInteger(usage.get("total_tokens")));
        return result;
    }

    private String normalizeRole(String role) {
        return switch (role) {
            case "assistant", "system" -> role;
            default -> "user";
        };
    }

    private String buildMessageContent(AiAssistantChatRequest.Message item) {
        String text = Objects.requireNonNullElse(item.text(), "");
        if (item.attachments() == null || item.attachments().isEmpty()) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        if (!text.isBlank()) {
            builder.append(text).append("\n\n");
        }
        builder.append("用户附带了图片文件，请在回答中说明你已收到图片，并基于用户提供的文字上下文继续交流。");
        builder.append(" 当前模型不直接解析图片像素内容，不要臆测图片细节；如需进一步判断，请引导用户补充图片关键信息。");
        builder.append("\n图片列表：");
        for (AiAssistantChatRequest.Attachment attachment : item.attachments()) {
            builder.append("\n- ")
                    .append(attachment.name())
                    .append(" (")
                    .append(attachment.mimeType())
                    .append(")");
        }
        return builder.toString();
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            rawMap.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asListOfMap(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                result.add(asMap(item));
            }
        }
        return result;
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String resolveConversationId(AiAssistantChatRequest request) {
        return request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString()
                : request.conversationId();
    }

    private void validateApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("AI 助手未配置 SiliconFlow API Key，请设置环境变量 SILICONFLOW_API_KEY");
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 240) {
            return value;
        }
        return value.substring(0, 240) + "...";
    }

    private String readResponseBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return body.string();
    }

    private boolean isInterrupted(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
