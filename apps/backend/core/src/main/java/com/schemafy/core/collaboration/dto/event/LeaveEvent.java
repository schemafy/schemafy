package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class LeaveEvent {

  private LeaveEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      String userId,
      String userName,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, String userId,
        String userName) {
      return new Outbound(sessionId, userId, userName,
          System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.LEAVE;
    }

    @Override
    public Outbound withoutSessionId() {
      return new Outbound(null, userId, userName, timestamp);
    }

  }

}
