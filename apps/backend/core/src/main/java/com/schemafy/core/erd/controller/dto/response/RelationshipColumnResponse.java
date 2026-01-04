package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RelationshipColumnResponse {

  private String id;
  private String relationshipId;
  private String fkColumnId;
  private String pkColumnId;
  private Integer seqNo;

  public static RelationshipColumnResponse from(
      RelationshipColumn relationshipColumn) {
    return RelationshipColumnResponse.builder()
        .id(relationshipColumn.getId())
        .relationshipId(relationshipColumn.getRelationshipId())
        .fkColumnId(relationshipColumn.getFkColumnId())
        .pkColumnId(relationshipColumn.getPkColumnId())
        .seqNo(relationshipColumn.getSeqNo())
        .build();
  }

}
