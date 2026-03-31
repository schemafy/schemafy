package com.schemafy.api.collaboration.dto.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

public final class ErdMutatedEvent {

  private ErdMutatedEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      String schemaId,
      Set<String> affectedTableIds,
      CommittedErdOperation operation,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId, String schemaId,
        Set<String> affectedTableIds,
        CommittedErdOperation operation) {
      return new Outbound(sessionId, schemaId, affectedTableIds,
          operation, System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.ERD_MUTATED;
    }

  }

}
