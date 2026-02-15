package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddRelationshipColumnRequest(
    @NotBlank(message = "pkColumnId는 필수입니다.") String pkColumnId,
    @NotBlank(message = "fkColumnId는 필수입니다.") String fkColumnId,
    Integer seqNo) {
}
