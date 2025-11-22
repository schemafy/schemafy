package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemoCommentRequest(
        @NotBlank(message = "memoId는 필수입니다.") String memoId,
        @NotBlank(message = "commentId는 필수입니다.") String commentId,
        @NotBlank(message = "body는 필수입니다.") String body) {
}
