package com.schemafy.core.erd.controller.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

public record CreateConstraintRequest(
    @NotBlank(message = "tableId는 필수입니다.") String tableId,
    @NotBlank(message = "name은 필수입니다.") String name,
    @NotNull(message = "kind는 필수입니다.") ConstraintKind kind,
    String checkExpr,
    String defaultExpr,
    List<CreateConstraintColumnRequest> columns) {
}
