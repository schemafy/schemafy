package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;

public record CreateTableRequest(
    @NotBlank(message = "schemaId는 필수입니다.") String schemaId,
    @NotBlank(message = "name은 필수입니다.") String name,
    String charset,
    String collation,
    @JsonObject(nullable = true)
    JsonNode extra) {
}
