package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeSchemaNameRequest(
    @NotBlank(message = "projectId는 필수입니다.") String projectId,
    @NotBlank(message = "newName은 필수입니다.") String newName) {
}
