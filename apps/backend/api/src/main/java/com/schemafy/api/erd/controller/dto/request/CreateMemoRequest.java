package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;

public record CreateMemoRequest(
    @NotBlank(message = "schemaId는 필수입니다.") String schemaId,
    @NotNull(message = "positions는 필수입니다.") @JsonObject JsonNode positions,
    @NotBlank(message = "body는 필수입니다.") String body) {
}
