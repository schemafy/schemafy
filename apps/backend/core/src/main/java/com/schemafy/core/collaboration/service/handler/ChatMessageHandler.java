package com.schemafy.core.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.schemafy.core.chat.service.ChatService;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.event.ChatEvent;
import com.schemafy.core.collaboration.dto.event.CollaborationInbound;
import com.schemafy.core.collaboration.service.SessionRegistry;
import com.schemafy.core.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ChatMessageHandler implements InboundMessageHandler {

    private final SessionRegistry sessionRegistry;
    private final ChatService chatService;

    @Override
    public CollaborationEventType supportedType() {
        return CollaborationEventType.CHAT;
    }

    @Override
    public Mono<Void> handle(MessageContext context,
            CollaborationInbound message) {
        if (!(message instanceof ChatEvent.Inbound chatMessage)) {
            log.warn(
                    "[ChatMessageHandler] Invalid message format: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        String content = chatMessage.content();
        if (content == null || content.isBlank()) {
            log.warn("[ChatMessageHandler] Empty chat content: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        SessionEntry entry = sessionRegistry
                .getSessionEntry(context.projectId(), context.sessionId())
                .orElse(null);

        if (entry == null) {
            log.warn("[ChatMessageHandler] Session not found: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        String authorId = entry.authInfo().getUserId();
        String authorName = entry.authInfo().getUserName();

        return chatService
                .saveAndPublish(context.projectId(), context.sessionId(),
                        authorId, authorName, content)
                .then();
    }

}
