package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class ChatEvent {

    private ChatEvent() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Inbound(String content) implements CollaborationInbound {

        @Override
        @JsonProperty("type")
        public CollaborationEventType type() {
            return CollaborationEventType.CHAT;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Outbound(
            String sessionId,
            String messageId,
            String userId,
            String userName,
            String content,
            long timestamp) implements CollaborationOutbound {

        public static Outbound of(String sessionId, String messageId,
                String userId, String userName, String content) {
            return new Outbound(sessionId, messageId, userId, userName, content,
                    System.currentTimeMillis());
        }

        @Override
        @JsonProperty("type")
        public CollaborationEventType type() {
            return CollaborationEventType.CHAT;
        }

        @Override
        public Outbound withoutSessionId() {
            return new Outbound(null, messageId, userId, userName, content,
                    timestamp);
        }
    }

}
