package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

public record RelationshipColumnResponse(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {

  public static RelationshipColumnResponse from(RelationshipColumn column) {
    return new RelationshipColumnResponse(
        column.id(),
        column.relationshipId(),
        column.pkColumnId(),
        column.fkColumnId(),
        column.seqNo());
  }

}
