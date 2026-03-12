package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record CreateIndexColumnRequest(
    @NotBlank(message = "columnId는 필수입니다.") String columnId,
    Integer seqNo,
    @NotNull(message = "sortDirection은 필수입니다.") SortDirection sortDirection) {
}
