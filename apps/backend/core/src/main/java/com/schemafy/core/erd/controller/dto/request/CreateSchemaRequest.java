package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSchemaRequest(
    @NotBlank(message = "projectId는 필수입니다.") String projectId,
    @NotBlank(message = "dbVendorName은 필수입니다.") String dbVendorName,
    @NotBlank(message = "name은 필수입니다.") String name,
    String charset,
    String collation) {
}
