package com.schemafy.core.erd.memo.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateMemoPositionCommand(
    String memoId,
    JsonNode positions,
    String requesterId) {
}
