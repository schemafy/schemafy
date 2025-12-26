package com.schemafy.core.collaboration.dto.event;

import com.schemafy.core.collaboration.dto.CursorPosition;

public final class CollaborationOutboundFactory {

    private CollaborationOutboundFactory() {}

    public static JoinEvent.Outbound join(String sessionId, String userId,
            String userName) {
        return JoinEvent.Outbound.of(sessionId, userId, userName);
    }

    public static LeaveEvent.Outbound leave(String sessionId, String userId,
            String userName) {
        return LeaveEvent.Outbound.of(sessionId, userId, userName);
    }

    public static CursorEvent.Outbound cursor(String sessionId,
            CursorPosition cursor) {
        return CursorEvent.Outbound.of(sessionId, cursor);
    }

    public static SchemaFocusEvent.Outbound schemaFocus(String sessionId,
            String userId, String userName, String schemaId) {
        return SchemaFocusEvent.Outbound.of(sessionId, userId, userName,
                schemaId);
    }

    public static ChatEvent.Outbound chat(String sessionId, String messageId,
            String userId, String userName, String content) {
        return ChatEvent.Outbound.of(sessionId, messageId, userId, userName,
                content);
    }

}

