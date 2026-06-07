package com.schemafy.api.collaboration.service.presence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectPresenceSession(
    String sessionId,
    String userId,
    String userName,
    long joinedAt,
    long lastSeenAt) {
}
