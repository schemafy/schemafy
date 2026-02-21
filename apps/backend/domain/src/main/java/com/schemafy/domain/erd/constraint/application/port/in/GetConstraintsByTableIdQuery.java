package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;

public record GetConstraintsByTableIdQuery(String tableId) {

  public GetConstraintsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
