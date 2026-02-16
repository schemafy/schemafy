package com.schemafy.core.collaboration.dto.event;

import java.util.Set;

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
      CursorEvent.UserInfo userInfo,
      CursorPosition cursor) {
    return CursorEvent.Outbound.of(sessionId, userInfo, cursor);
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

  public static ErdMutatedEvent.Outbound erdMutated(String sessionId,
      String schemaId, Set<String> affectedTableIds) {
    return ErdMutatedEvent.Outbound.of(sessionId, schemaId,
        affectedTableIds);
  }

}
