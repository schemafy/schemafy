package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTableRequest(
    @NotBlank(message = "schemaId는 필수입니다.") String schemaId,
    @NotBlank(message = "name은 필수입니다.") String name,
    String charset,
    String collation) {
}
