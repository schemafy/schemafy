package com.schemafy.api.collaboration.dto.event;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.collaboration.dto.CursorPosition;
import com.schemafy.api.collaboration.dto.PreviewAction;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

public final class CollaborationOutboundFactory {

  private CollaborationOutboundFactory() {}

  public static SessionReadyEvent.Outbound sessionReady(String sessionId) {
    return SessionReadyEvent.Outbound.of(sessionId);
  }

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

  public static TablePositionPreviewEvent.Outbound tablePositionPreview(
      String sessionId, PreviewAction action, String schemaId, String tableId,
      JsonNode position) {
    return TablePositionPreviewEvent.Outbound.of(sessionId, action, schemaId,
        tableId, position);
  }

  public static RelationshipExtraPreviewEvent.Outbound relationshipExtraPreview(
      String sessionId, PreviewAction action, String schemaId,
      String relationshipId, JsonNode extra) {
    return RelationshipExtraPreviewEvent.Outbound.of(sessionId, action,
        schemaId, relationshipId, extra);
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
      String schemaId,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return ErdMutatedEvent.Outbound.of(sessionId, schemaId,
        affectedTableIds, operation);
  }

}
