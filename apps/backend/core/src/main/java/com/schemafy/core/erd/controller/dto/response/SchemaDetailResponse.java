package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

import com.schemafy.core.erd.repository.entity.Schema;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaDetailResponse {

    private String id;
    private String projectId;
    private String name;
    private List<TableResponse> tables;

    public static SchemaDetailResponse from(Schema schema, List<TableResponse> tables) {
        return SchemaDetailResponse.builder()
                .id(schema.getId())
                .projectId(schema.getProjectId())
                .name(schema.getName())
                .tables(tables)
                .build();
    }
}
