package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.CursorPosition;

public final class CursorEvent {

  private CursorEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Inbound(CursorPosition cursor)
      implements CollaborationInbound {

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.CURSOR;
    }

  }

  public record UserInfo(String userId, String userName) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      UserInfo userInfo,
      CursorPosition cursor,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, UserInfo userInfo,
        CursorPosition cursor) {
      return new Outbound(sessionId, userInfo, cursor,
          System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.CURSOR;
    }

    @Override
    public Outbound withoutSessionId() {
      return new Outbound(null, userInfo, cursor, timestamp);
    }

  }

}
