package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

import com.schemafy.core.erd.repository.entity.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableDetailResponse {

    private String id;
    private String schemaId;
    private String name;
    private String extra;
    private List<ColumnResponse> columns;
    private List<ConstraintResponse> constraints;
    private List<IndexResponse> indexes;
    private List<RelationshipResponse> relationships;

    public static TableDetailResponse from(Table table, List<ColumnResponse> columns,
            List<ConstraintResponse> constraints, List<IndexResponse> indexes,
            List<RelationshipResponse> relationships) {
        return TableDetailResponse.builder()
                .id(table.getId())
                .schemaId(table.getSchemaId())
                .name(table.getName())
                .extra(table.getExtra())
                .columns(columns)
                .constraints(constraints)
                .indexes(indexes)
                .relationships(relationships)
                .build();
    }
}
