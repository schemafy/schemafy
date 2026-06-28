package com.schemafy.api.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProjectPresenceParticipant(
    String sessionId,
    String userId,
    String userName,
    String profileImageUrl) {
}
