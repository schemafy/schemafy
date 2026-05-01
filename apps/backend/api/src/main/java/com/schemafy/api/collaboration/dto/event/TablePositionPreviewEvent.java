package com.schemafy.api.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;

public final class TablePositionPreviewEvent {

  private TablePositionPreviewEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Inbound(
      PreviewAction action,
      String schemaId,
      String tableId,
      JsonNode position) implements CollaborationInbound {

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.TABLE_POSITION_PREVIEW;
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      PreviewAction action,
      String schemaId,
      String tableId,
      JsonNode position,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, PreviewAction action,
        String schemaId, String tableId, JsonNode position) {
      return new Outbound(sessionId, action, schemaId, tableId, position,
          System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.TABLE_POSITION_PREVIEW;
    }

  }

}
