package com.schemafy.domain.erd.relationship.application.port.in;

public record GetRelationshipColumnQuery(String relationshipColumnId) {

  public GetRelationshipColumnQuery {
    if (relationshipColumnId == null || relationshipColumnId.isBlank()) {
      throw new IllegalArgumentException("relationshipColumnId must not be blank");
    }
  }

}
