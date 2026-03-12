package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

public record GetRelationshipQuery(String relationshipId) {

  public GetRelationshipQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE, "relationshipId must not be blank");
    }
  }

}
