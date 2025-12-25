package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemoRequest(
    @NotBlank(message = "positions는 필수입니다.") String positions) {
}
