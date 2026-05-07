package com.waterlevel.server.service;

import com.waterlevel.server.dto.AiAssistantChatRequest;
import com.waterlevel.server.dto.AiAssistantChatResponse;

import java.util.Map;
import java.util.function.Consumer;

public interface AiAssistantService {

    AiAssistantChatResponse chat(AiAssistantChatRequest request);

    void streamChat(AiAssistantChatRequest request, Consumer<Map<String, Object>> eventConsumer);
}
