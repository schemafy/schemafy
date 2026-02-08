package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeColumnTypeRequest(
    @NotBlank(message = "dataType은 필수입니다.") String dataType,
    Integer length,
    Integer precision,
    Integer scale) {
}
