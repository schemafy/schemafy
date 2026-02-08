package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.index.domain.type.IndexType;

public record ChangeIndexTypeRequest(
    @NotNull(message = "type은 필수입니다.") IndexType type) {
}
