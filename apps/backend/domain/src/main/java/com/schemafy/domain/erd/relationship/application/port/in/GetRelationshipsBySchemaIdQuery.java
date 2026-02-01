package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetRelationshipsBySchemaIdQuery(String schemaId) {

  public GetRelationshipsBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new InvalidValueException("schemaId must not be blank");
    }
  }

}
