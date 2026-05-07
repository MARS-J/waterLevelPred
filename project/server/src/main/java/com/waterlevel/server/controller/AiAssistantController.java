package com.waterlevel.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterlevel.server.common.ApiResponse;
import com.waterlevel.server.dto.AiAssistantChatRequest;
import com.waterlevel.server.dto.AiAssistantChatResponse;
import com.waterlevel.server.service.AiAssistantService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;

    public AiAssistantController(AiAssistantService aiAssistantService, ObjectMapper objectMapper) {
        this.aiAssistantService = aiAssistantService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public ApiResponse<AiAssistantChatResponse> chat(@Valid @RequestBody AiAssistantChatRequest request) {
        return ApiResponse.success(aiAssistantService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(
            @Valid @RequestBody AiAssistantChatRequest request,
            HttpServletResponse response
    ) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return outputStream -> aiAssistantService.streamChat(request, event ->
                writeEvent(outputStream, String.valueOf(event.getOrDefault("type", "message")), event)
        );
    }

    private void writeEvent(OutputStream outputStream, String eventName, Map<String, Object> payload) {
        try {
            String body = "event: " + eventName + "\n" +
                    "data: " + objectMapper.writeValueAsString(payload) + "\n\n";
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("写入流式响应失败", exception);
        }
    }
}
