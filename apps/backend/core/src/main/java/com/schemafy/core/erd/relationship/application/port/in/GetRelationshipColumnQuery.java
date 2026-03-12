package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

public record GetRelationshipColumnQuery(String relationshipColumnId) {

  public GetRelationshipColumnQuery {
    if (relationshipColumnId == null || relationshipColumnId.isBlank()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE, "relationshipColumnId must not be blank");
    }
  }

}
