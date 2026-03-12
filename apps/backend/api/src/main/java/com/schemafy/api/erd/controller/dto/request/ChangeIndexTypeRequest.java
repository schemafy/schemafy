package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record ChangeIndexTypeRequest(
    @NotNull(message = "type은 필수입니다.") IndexType type) {
}
