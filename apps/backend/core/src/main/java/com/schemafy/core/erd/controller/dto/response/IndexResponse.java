package com.schemafy.core.erd.controller.dto.response;

import java.util.Collections;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;

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
    private List<IndexColumnResponse> columns;

    public static IndexResponse from(Index index) {
        return IndexResponse.builder()
                .id(index.getId())
                .tableId(index.getTableId())
                .name(index.getName())
                .type(index.getType())
                .comment(index.getComment())
                .columns(Collections.emptyList())
                .build();
    }

    public static IndexResponse from(Index index, List<IndexColumn> columns) {
        return IndexResponse.builder()
                .id(index.getId())
                .tableId(index.getTableId())
                .name(index.getName())
                .type(index.getType())
                .comment(index.getComment())
                .columns(columns.stream()
                        .map(IndexColumnResponse::from)
                        .toList())
                .build();
    }

}
