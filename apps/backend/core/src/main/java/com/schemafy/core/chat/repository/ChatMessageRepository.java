package com.schemafy.core.chat.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.chat.repository.entity.ChatMessage;

import reactor.core.publisher.Flux;

public interface ChatMessageRepository
        extends ReactiveCrudRepository<ChatMessage, String> {

    @Query("SELECT * FROM chat_messages WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findByProjectIdOrderByCreatedAtDesc(String projectId,
            int limit);

    @Query("SELECT * FROM chat_messages WHERE project_id = :projectId AND id < :beforeId AND deleted_at IS NULL ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findByProjectIdAndIdLessThanOrderByCreatedAtDesc(
            String projectId, String beforeId, int limit);

}

