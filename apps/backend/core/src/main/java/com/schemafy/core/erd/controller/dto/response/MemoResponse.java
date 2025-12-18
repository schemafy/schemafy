package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoResponse {

    private String id;
    private String schemaId;
    private UserInfoResponse author;
    private String positions;
    private Instant createdAt;
    private Instant updatedAt;

    public static MemoResponse from(Memo memo, UserInfoResponse author) {
        return MemoResponse.builder()
                .id(memo.getId())
                .schemaId(memo.getSchemaId())
                .author(author)
                .positions(memo.getPositions())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .build();
    }

}
