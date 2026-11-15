package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.core.collaboration.dto.CanvasEditLockAction;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.lock.CanvasEditLockState;
import com.schemafy.core.collaboration.lock.CanvasEditLockTarget;

public final class CanvasEditLockEvent {

  private CanvasEditLockEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Inbound(
      CanvasEditLockAction action,
      CanvasEditLockTarget target,
      String schemaId,
      String resourceId) implements CollaborationInbound {

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.CANVAS_EDIT_LOCK;
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      CanvasEditLockState state,
      CanvasEditLockTarget target,
      String schemaId,
      String resourceId,
      String ownerSessionId,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, CanvasEditLockState state,
        CanvasEditLockTarget target, String schemaId, String resourceId,
        String ownerSessionId) {
      return new Outbound(sessionId, state, target, schemaId, resourceId,
          ownerSessionId, System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.CANVAS_EDIT_LOCK;
    }

  }

}
