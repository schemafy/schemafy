package com.schemafy.domain.erd.relationship.application.port.in;

public record GetRelationshipsBySchemaIdQuery(String schemaId) {

  public GetRelationshipsBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new IllegalArgumentException("schemaId must not be blank");
    }
  }

}
