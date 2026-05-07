package com.waterlevel.server.dto;

public record AiAssistantChatResponse(
        String conversationId,
        String model,
        String reply,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
