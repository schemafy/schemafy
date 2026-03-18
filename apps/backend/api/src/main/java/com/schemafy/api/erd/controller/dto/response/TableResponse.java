package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record TableResponse(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation,
    JsonNode extra) {
}
