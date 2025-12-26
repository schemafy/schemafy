package com.schemafy.core.chat.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("chat_messages")
public class ChatMessage extends BaseEntity {

    private String projectId;

    private String authorId;

    private String body;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static ChatMessage newChatMessage(String projectId, String authorId,
            String body) {
        ChatMessage message = new ChatMessage(projectId, authorId, body);
        message.setId(UlidGenerator.generate());
        return message;
    }

}
