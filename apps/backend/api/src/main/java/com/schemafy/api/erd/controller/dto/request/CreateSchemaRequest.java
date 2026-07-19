package com.schemafy.api.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSchemaRequest(
    @NotBlank(message = "projectId는 필수입니다.") String projectId,
    @NotBlank(message = "name은 필수입니다.") String name,
    String charset,
    String collation) {
}
