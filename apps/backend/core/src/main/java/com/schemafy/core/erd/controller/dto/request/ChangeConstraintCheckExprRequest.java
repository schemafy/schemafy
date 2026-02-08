package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeConstraintCheckExprRequest(
    @NotBlank(message = "checkExpr는 필수입니다.") String checkExpr) {
}
