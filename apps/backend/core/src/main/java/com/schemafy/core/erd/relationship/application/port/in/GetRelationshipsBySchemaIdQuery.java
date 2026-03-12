package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

public record GetRelationshipsBySchemaIdQuery(String schemaId) {

  public GetRelationshipsBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE, "schemaId must not be blank");
    }
  }

}
