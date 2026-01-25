package com.schemafy.domain.erd.relationship.application.port.in;

public record GetRelationshipQuery(String relationshipId) {

  public GetRelationshipQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new IllegalArgumentException("relationshipId must not be blank");
    }
  }

}
