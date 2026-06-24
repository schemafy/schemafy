package com.schemafy.api.collaboration.dto.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.ProjectPresenceParticipant;

public final class SessionReadyEvent {

  private SessionReadyEvent() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Outbound(
      String sessionId,
      List<ProjectPresenceParticipant> participants,
      long timestamp) implements CollaborationOutbound {

    public static Outbound of(String sessionId,
        List<ProjectPresenceParticipant> participants) {
      List<ProjectPresenceParticipant> snapshot = List.copyOf(participants);
      return new Outbound(sessionId, snapshot, System.currentTimeMillis());
    }

    @Override
    public CollaborationEventType type() {
      return CollaborationEventType.SESSION_READY;
    }

  }

}
