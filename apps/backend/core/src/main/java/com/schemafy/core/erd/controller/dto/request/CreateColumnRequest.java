package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateColumnRequest(
    @NotBlank(message = "tableId는 필수입니다.") String tableId,
    @NotBlank(message = "name은 필수입니다.") String name,
    @NotBlank(message = "dataType은 필수입니다.") String dataType,
    Integer length,
    Integer precision,
    Integer scale,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
