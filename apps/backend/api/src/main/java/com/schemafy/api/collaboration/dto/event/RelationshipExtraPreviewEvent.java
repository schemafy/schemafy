package com.schemafy.api.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;

public final class RelationshipExtraPreviewEvent {

  private RelationshipExtraPreviewEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Inbound(
      PreviewAction action,
      String schemaId,
      String relationshipId,
      JsonNode extra) implements CollaborationInbound {

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.RELATIONSHIP_EXTRA_PREVIEW;
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      PreviewAction action,
      String schemaId,
      String relationshipId,
      JsonNode extra,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, PreviewAction action,
        String schemaId, String relationshipId, JsonNode extra) {
      return new Outbound(sessionId, action, schemaId, relationshipId, extra,
          System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.RELATIONSHIP_EXTRA_PREVIEW;
    }

  }

}
