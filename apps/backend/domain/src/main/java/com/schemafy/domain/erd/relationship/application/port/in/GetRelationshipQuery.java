package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetRelationshipQuery(String relationshipId) {

  public GetRelationshipQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new InvalidValueException("relationshipId must not be blank");
    }
  }

}
