package com.schemafy.core.erd.table.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record CreateTableCommand(
    String schemaId,
    String name,
    String charset,
    String collation,
    JsonNode extra) {
}
