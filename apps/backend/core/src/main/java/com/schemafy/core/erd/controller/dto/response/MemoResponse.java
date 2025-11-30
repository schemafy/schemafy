package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.Memo;

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
    private String authorId;
    private String positions;
    private Instant createdAt;
    private Instant updatedAt;

    public static MemoResponse from(Memo memo) {
        return MemoResponse.builder()
                .id(memo.getId())
                .schemaId(memo.getSchemaId())
                .authorId(memo.getAuthorId())
                .positions(memo.getPositions())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .build();
    }

}
