package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnResult;

public record AddRelationshipColumnResponse(
    String id,
    String relationshipId,
    String pkColumnId,
    String fkColumnId,
    int seqNo) {

  public static AddRelationshipColumnResponse from(AddRelationshipColumnResult result) {
    return new AddRelationshipColumnResponse(
        result.relationshipColumnId(),
        result.relationshipId(),
        result.pkColumnId(),
        result.fkColumnId(),
        result.seqNo());
  }

}
