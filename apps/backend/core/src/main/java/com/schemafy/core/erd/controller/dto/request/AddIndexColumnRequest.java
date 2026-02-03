package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.index.domain.type.SortDirection;

public record AddIndexColumnRequest(
    @NotBlank(message = "columnId는 필수입니다.") String columnId,
    int seqNo,
    @NotNull(message = "sortDirection은 필수입니다.") SortDirection sortDirection) {
}
