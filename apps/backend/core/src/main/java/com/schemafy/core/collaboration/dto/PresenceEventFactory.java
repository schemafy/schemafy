package com.schemafy.core.collaboration.dto;

public final class PresenceEventFactory {

    private PresenceEventFactory() {}

    public static PresenceJoinEvent join(String sessionId, String userId,
            String userName) {
        return PresenceJoinEvent.of(sessionId, userId, userName);
    }

    public static PresenceLeaveEvent leave(String sessionId, String userId,
            String userName) {
        return PresenceLeaveEvent.of(sessionId, userId, userName);
    }

    public static PresenceCursorEvent cursor(String sessionId,
            CursorPosition cursor) {
        return PresenceCursorEvent.of(sessionId, cursor);
    }

}
