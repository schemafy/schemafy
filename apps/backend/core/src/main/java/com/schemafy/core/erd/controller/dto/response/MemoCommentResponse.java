package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.MemoComment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoCommentResponse {

    private String id;
    private String memoId;
    private String authorId;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;

    public static MemoCommentResponse from(MemoComment memoComment) {
        return MemoCommentResponse.builder()
                .id(memoComment.getId())
                .memoId(memoComment.getMemoId())
                .authorId(memoComment.getAuthorId())
                .body(memoComment.getBody())
                .createdAt(memoComment.getCreatedAt())
                .updatedAt(memoComment.getUpdatedAt())
                .build();
    }

}
