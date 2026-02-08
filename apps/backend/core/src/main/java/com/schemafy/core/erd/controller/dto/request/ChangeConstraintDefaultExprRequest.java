package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeConstraintDefaultExprRequest(
    @NotBlank(message = "defaultExpr는 필수입니다.") String defaultExpr) {
}
