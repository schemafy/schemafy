package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.MemoComment;
import com.schemafy.core.user.controller.dto.response.UserSummaryResponse;

public record MemoCommentResponse(
        String id,
        String memoId,
        UserSummaryResponse author,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public static MemoCommentResponse from(MemoComment memoComment,
            UserSummaryResponse author) {
        return new MemoCommentResponse(
                memoComment.getId(),
                memoComment.getMemoId(),
                author,
                memoComment.getBody(),
                memoComment.getCreatedAt(),
                memoComment.getUpdatedAt());
    }

}
