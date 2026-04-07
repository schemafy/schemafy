package com.schemafy.core.project.application.port.in;

import java.util.Locale;

import com.schemafy.core.ulid.application.service.UlidGenerator;

public record GetMyInvitationsQuery(
    String requesterId,
    String cursorId,
    int size) {

  public GetMyInvitationsQuery {
    if (cursorId == null || cursorId.isBlank()) {
      cursorId = null;
    } else {
      cursorId = cursorId.trim().toUpperCase(Locale.ROOT);
      UlidGenerator.extractTimestamp(cursorId);
    }
  }

}
