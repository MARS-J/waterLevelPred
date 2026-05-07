package com.waterlevel.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiAssistantChatRequest(
        String conversationId,
        @NotNull
        @NotEmpty
        List<@Valid Message> messages
) {
    public record Message(
            @NotNull
            String role,
            String text,
            List<@Valid Attachment> attachments
    ) {
    }

    public record Attachment(
            @NotNull
            String name,
            @NotNull
            String mimeType,
            @NotNull
            String dataUrl
    ) {
    }
}
