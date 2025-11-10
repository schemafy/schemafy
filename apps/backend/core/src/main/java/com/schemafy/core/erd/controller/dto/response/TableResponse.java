package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.erd.repository.entity.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableResponse {

    private String id;
    private String schemaId;
    private String name;
    private String comment;
    private String tableOptions;
    private String extra;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public static TableResponse from(Table table) {
        return TableResponse.builder()
                .id(table.getId())
                .schemaId(table.getSchemaId())
                .name(table.getName())
                .comment(table.getComment())
                .tableOptions(table.getTableOptions())
                .extra(table.getExtra())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .deletedAt(table.getDeletedAt())
                .build();
    }
}
