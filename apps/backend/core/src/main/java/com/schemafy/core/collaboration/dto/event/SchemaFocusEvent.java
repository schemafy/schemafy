package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class SchemaFocusEvent {

    private SchemaFocusEvent() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Inbound(String schemaId) implements CollaborationInbound {

        @Override
        @JsonProperty("type")
        public CollaborationEventType type() {
            return CollaborationEventType.SCHEMA_FOCUS;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Outbound(
            String sessionId,
            String userId,
            String userName,
            String schemaId,
            long timestamp) implements CollaborationOutbound {

        public static Outbound of(String sessionId, String userId,
                String userName, String schemaId) {
            return new Outbound(sessionId, userId, userName, schemaId,
                    System.currentTimeMillis());
        }

        @Override
        @JsonProperty("type")
        public CollaborationEventType type() {
            return CollaborationEventType.SCHEMA_FOCUS;
        }

        @Override
        public Outbound withoutSessionId() {
            return new Outbound(null, userId, userName, schemaId, timestamp);
        }
    }

}
