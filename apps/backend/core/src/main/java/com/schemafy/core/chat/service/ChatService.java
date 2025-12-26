package com.schemafy.core.chat.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.chat.repository.ChatMessageRepository;
import com.schemafy.core.chat.repository.entity.ChatMessage;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.event.ChatEvent;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Mono<ChatMessage> saveAndPublish(String projectId, String sessionId,
            String authorId, String userName, String content) {
        ChatMessage message = ChatMessage.builder()
                .projectId(projectId)
                .authorId(authorId)
                .body(content)
                .build();

        return chatMessageRepository.save(message)
                .flatMap(savedMessage -> publishToRedis(projectId, sessionId,
                        savedMessage, userName)
                        .thenReturn(savedMessage))
                .doOnSuccess(msg -> log.debug(
                        "[ChatService] Message saved and published: messageId={}, projectId={}",
                        msg.getId(), projectId))
                .doOnError(e -> log.error(
                        "[ChatService] Failed to save message: projectId={}, error={}",
                        projectId, e.getMessage()));
    }

    private Mono<Void> publishToRedis(String projectId, String sessionId,
            ChatMessage message, String userName) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        ChatEvent.Outbound event = CollaborationOutboundFactory.chat(
                sessionId,
                message.getId(),
                message.getAuthorId(),
                userName,
                message.getBody());

        return serializeToJson(event)
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .doOnError(e -> log.warn(
                        "[ChatService] Failed to publish chat to Redis: messageId={}, error={}",
                        message.getId(), e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    public Flux<ChatMessage> getMessages(String projectId, String beforeId,
            Integer limit) {
        int pageSize = resolvePageSize(limit);

        if (beforeId != null && !beforeId.isBlank()) {
            return chatMessageRepository
                    .findByProjectIdAndIdLessThanOrderByCreatedAtDesc(projectId,
                            beforeId, pageSize);
        }

        return chatMessageRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId, pageSize);
    }

    private int resolvePageSize(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private Mono<String> serializeToJson(Object object) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(object))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[ChatService] failed to serialize JSON", e));
    }

}
