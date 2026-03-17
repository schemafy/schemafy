package com.schemafy.api.erd.controller.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;

public record ChangeTableExtraRequest(
    @JsonObject(nullable = true)
    JsonNode extra) {
}
