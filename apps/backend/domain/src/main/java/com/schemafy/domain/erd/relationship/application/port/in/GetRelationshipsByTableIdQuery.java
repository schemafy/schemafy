package com.schemafy.domain.erd.relationship.application.port.in;

public record GetRelationshipsByTableIdQuery(String tableId) {

  public GetRelationshipsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }

}
