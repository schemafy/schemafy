package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoDetailResponse {

    private String id;
    private String schemaId;
    private UserInfoResponse author;
    private String positions;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MemoCommentResponse> comments;

    public static MemoDetailResponse from(Memo memo,
            List<MemoCommentResponse> comments, UserInfoResponse author) {
        return MemoDetailResponse.builder()
                .id(memo.getId())
                .schemaId(memo.getSchemaId())
                .author(author)
                .positions(memo.getPositions())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .comments(comments)
                .build();
    }

}
