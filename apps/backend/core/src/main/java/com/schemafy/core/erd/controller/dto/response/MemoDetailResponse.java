package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Memo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoDetailResponse {

    private String id;
    private String schemaId;
    private String authorId;
    private String positions;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private List<MemoCommentResponse> comments;

    public static MemoDetailResponse from(Memo memo,
            List<MemoCommentResponse> comments) {
        return MemoDetailResponse.builder()
                .id(memo.getId())
                .schemaId(memo.getSchemaId())
                .authorId(memo.getAuthorId())
                .positions(memo.getPositions())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .deletedAt(memo.getDeletedAt())
                .comments(comments)
                .build();
    }

}
