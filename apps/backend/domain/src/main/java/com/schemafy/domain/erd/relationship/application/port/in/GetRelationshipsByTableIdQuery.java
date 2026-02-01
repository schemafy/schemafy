package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetRelationshipsByTableIdQuery(String tableId) {

  public GetRelationshipsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new InvalidValueException("tableId must not be blank");
    }
  }

}
