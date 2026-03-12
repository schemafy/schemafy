package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;

public record GetConstraintsByTableIdQuery(String tableId) {

  public GetConstraintsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
