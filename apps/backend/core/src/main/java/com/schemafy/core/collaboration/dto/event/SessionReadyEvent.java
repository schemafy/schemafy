package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class SessionReadyEvent {

  private SessionReadyEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId) {
      return new Outbound(sessionId, System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.SESSION_READY;
    }

  }

}
