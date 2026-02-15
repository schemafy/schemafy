package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetRelationshipColumnsByRelationshipIdQuery(String relationshipId) {

  public GetRelationshipColumnsByRelationshipIdQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new InvalidValueException("relationshipId must not be blank");
    }
  }

}
