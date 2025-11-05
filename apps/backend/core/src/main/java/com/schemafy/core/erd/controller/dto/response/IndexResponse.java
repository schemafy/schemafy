package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.Index;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexResponse {

    private String id;
    private String tableId;
    private String name;
    private String type;
    private String comment;

    public static IndexResponse from(Index index) {
        return IndexResponse.builder()
                .id(index.getId())
                .tableId(index.getTableId())
                .name(index.getName())
                .type(index.getType())
                .comment(index.getComment())
                .build();
    }
}
