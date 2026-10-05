package com.schemafy.core.erd.table.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record ChangeTablePositionCommand(
    String tableId,
    JsonNode position) {
}
