package com.schemafy.core.chat.controller.dto;

import java.time.Instant;

import com.schemafy.core.chat.repository.entity.ChatMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String id;
    private String projectId;
    private String authorId;
    private String body;
    private Instant createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .projectId(message.getProjectId())
                .authorId(message.getAuthorId())
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .build();
    }

}
