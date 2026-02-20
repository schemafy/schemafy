package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;

public record GetRelationshipQuery(String relationshipId) {

  public GetRelationshipQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE, "relationshipId must not be blank");
    }
  }

}
