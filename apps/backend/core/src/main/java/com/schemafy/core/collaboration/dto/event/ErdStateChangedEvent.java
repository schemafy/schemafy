package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

public final class ErdStateChangedEvent {

  private ErdStateChangedEvent() {}

  public enum State {
    ACTIVE,
    DELETED
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      String schemaId,
      long revision,
      State state,
      @JsonInclude(JsonInclude.Include.ALWAYS) JsonNode schema,
      @JsonInclude(JsonInclude.Include.ALWAYS) JsonNode snapshots,
      long timestamp) implements CollaborationOutbound {

    public Outbound {
      schema = schema != null && schema.isNull() ? null : schema;
      snapshots = snapshots != null && snapshots.isNull() ? null : snapshots;
    }

    public static Outbound active(String schemaId, long revision,
        JsonNode schema, JsonNode snapshots) {
      return new Outbound(null, schemaId, revision, State.ACTIVE, schema,
          snapshots, System.currentTimeMillis());
    }

    public static Outbound deleted(String schemaId, long revision) {
      return new Outbound(null, schemaId, revision, State.DELETED, null, null,
          System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.ERD_STATE_CHANGED;
    }

  }

}
