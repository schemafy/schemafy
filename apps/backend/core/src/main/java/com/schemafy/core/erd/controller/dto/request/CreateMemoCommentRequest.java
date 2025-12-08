package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMemoCommentRequest(
        @NotBlank(message = "내용은 필수입니다.") String body) {
}
