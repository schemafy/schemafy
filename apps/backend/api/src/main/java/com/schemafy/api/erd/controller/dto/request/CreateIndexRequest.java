package com.schemafy.api.erd.controller.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record CreateIndexRequest(
    @NotBlank(message = "tableId는 필수입니다.") String tableId,
    String name,
    @NotNull(message = "type은 필수입니다.") IndexType type,
    List<CreateIndexColumnRequest> columns) {
}
