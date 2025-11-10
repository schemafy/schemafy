package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RelationshipColumnResponse {

    private String id;
    private String relationshipId;
    private String srcColumnId;
    private String tgtColumnId;
    private Integer seqNo;

    public static RelationshipColumnResponse from(RelationshipColumn relationshipColumn) {
        return RelationshipColumnResponse.builder()
                .id(relationshipColumn.getId())
                .relationshipId(relationshipColumn.getRelationshipId())
                .srcColumnId(relationshipColumn.getSrcColumnId())
                .tgtColumnId(relationshipColumn.getTgtColumnId())
                .seqNo(relationshipColumn.getSeqNo())
                .build();
    }
}
