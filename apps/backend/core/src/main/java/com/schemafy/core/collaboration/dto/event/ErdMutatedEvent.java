package com.schemafy.core.collaboration.dto.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class ErdMutatedEvent {

  private ErdMutatedEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      String schemaId,
      Set<String> affectedTableIds,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, String schemaId,
        Set<String> affectedTableIds) {
      return new Outbound(sessionId, schemaId, affectedTableIds,
          System.currentTimeMillis());
    }

    public static Outbound of(String schemaId,
        Set<String> affectedTableIds) {
      return of(null, schemaId, affectedTableIds);
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.ERD_MUTATED;
    }

    @Override
    public Outbound withoutSessionId() {
      return new Outbound(null, schemaId, affectedTableIds, timestamp);
    }

  }

}
