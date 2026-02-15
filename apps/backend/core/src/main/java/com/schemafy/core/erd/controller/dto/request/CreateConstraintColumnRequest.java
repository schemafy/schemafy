package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateConstraintColumnRequest(
    @NotBlank(message = "columnId는 필수입니다.") String columnId,
    Integer seqNo) {
}
