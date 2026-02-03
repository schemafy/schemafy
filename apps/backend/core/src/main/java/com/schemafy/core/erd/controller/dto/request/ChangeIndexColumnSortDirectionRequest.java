package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.index.domain.type.SortDirection;

public record ChangeIndexColumnSortDirectionRequest(
    @NotNull(message = "sortDirection은 필수입니다.") SortDirection sortDirection) {
}
