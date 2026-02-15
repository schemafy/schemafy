package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeConstraintNameRequest(
    @NotBlank(message = "newName은 필수입니다.") String newName) {
}
