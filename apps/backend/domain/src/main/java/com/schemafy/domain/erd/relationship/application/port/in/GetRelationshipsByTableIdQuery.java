package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;

public record GetRelationshipsByTableIdQuery(String tableId) {

  public GetRelationshipsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
