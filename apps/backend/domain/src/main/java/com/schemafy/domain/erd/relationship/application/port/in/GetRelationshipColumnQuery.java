package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetRelationshipColumnQuery(String relationshipColumnId) {

  public GetRelationshipColumnQuery {
    if (relationshipColumnId == null || relationshipColumnId.isBlank()) {
      throw new InvalidValueException("relationshipColumnId must not be blank");
    }
  }

}
