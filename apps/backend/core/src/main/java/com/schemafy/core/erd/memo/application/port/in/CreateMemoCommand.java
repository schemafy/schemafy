package com.schemafy.core.erd.memo.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record CreateMemoCommand(
    String schemaId,
    JsonNode positions,
    String body,
    String authorId) {
}
