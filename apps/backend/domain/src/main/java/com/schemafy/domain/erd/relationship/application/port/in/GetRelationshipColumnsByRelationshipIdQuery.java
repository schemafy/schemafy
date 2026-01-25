package com.schemafy.domain.erd.relationship.application.port.in;

public record GetRelationshipColumnsByRelationshipIdQuery(String relationshipId) {

  public GetRelationshipColumnsByRelationshipIdQuery {
    if (relationshipId == null || relationshipId.isBlank()) {
      throw new IllegalArgumentException("relationshipId must not be blank");
    }
  }

}
