package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.common.annotation.JsonObject;

public record UpdateMemoRequest(
    @NotNull(message = "positions는 필수입니다.") @JsonObject JsonNode positions) {
}
