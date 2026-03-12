package com.schemafy.api.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.api.user.controller.dto.response.UserSummaryResponse;

public record MemoCommentResponse(
    String id,
    String memoId,
    UserSummaryResponse author,
    String body,
    Instant createdAt,
    Instant updatedAt) {
}
