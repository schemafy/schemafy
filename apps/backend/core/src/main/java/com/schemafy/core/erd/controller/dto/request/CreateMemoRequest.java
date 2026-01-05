package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMemoRequest(
    @NotBlank(message = "schemaId는 필수입니다.") String schemaId,
    @NotBlank(message = "positions는 필수입니다.") String positions,
    @NotBlank(message = "body는 필수입니다.") String body) {
}
