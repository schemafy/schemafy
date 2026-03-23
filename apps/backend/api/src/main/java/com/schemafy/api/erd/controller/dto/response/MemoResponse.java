package com.schemafy.api.erd.controller.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.user.controller.dto.response.UserSummaryResponse;

public record MemoResponse(
    String id,
    String schemaId,
    UserSummaryResponse author,
    JsonNode positions,
    Instant createdAt,
    Instant updatedAt) {
}
