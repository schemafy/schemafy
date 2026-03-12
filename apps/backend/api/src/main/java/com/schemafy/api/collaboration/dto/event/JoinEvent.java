package com.schemafy.api.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.api.collaboration.dto.CollaborationEventType;

public final class JoinEvent {

  private JoinEvent() {}

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
      return CollaborationEventType.JOIN;
    }

  }

}
