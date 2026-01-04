package com.schemafy.core.erd.controller.dto.response;

import java.util.Collections;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RelationshipResponse {

    private String id;
    private String fkTableId;
    private String pkTableId;
    private String name;
    private String kind;
    private String cardinality;
    private String onDelete;
    private String onUpdate;
    private String extra;
    private List<RelationshipColumnResponse> columns;

    public static RelationshipResponse from(Relationship relationship) {
        return RelationshipResponse.builder()
                .id(relationship.getId())
                .fkTableId(relationship.getFkTableId())
                .pkTableId(relationship.getPkTableId())
                .name(relationship.getName())
                .kind(relationship.getKind())
                .cardinality(relationship.getCardinality())
                .onDelete(relationship.getOnDelete())
                .onUpdate(relationship.getOnUpdate())
                .extra(relationship.getExtra())
                .columns(Collections.emptyList())
                .build();
    }

    public static RelationshipResponse from(Relationship relationship,
            List<RelationshipColumn> columns) {
        return RelationshipResponse.builder()
                .id(relationship.getId())
                .fkTableId(relationship.getFkTableId())
                .pkTableId(relationship.getPkTableId())
                .name(relationship.getName())
                .kind(relationship.getKind())
                .cardinality(relationship.getCardinality())
                .onDelete(relationship.getOnDelete())
                .onUpdate(relationship.getOnUpdate())
                .extra(relationship.getExtra())
                .columns(columns.stream()
                        .map(RelationshipColumnResponse::from)
                        .toList())
                .build();
    }

}
