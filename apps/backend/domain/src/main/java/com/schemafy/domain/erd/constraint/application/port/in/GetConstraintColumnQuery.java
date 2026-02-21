package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;

public record GetConstraintColumnQuery(String constraintColumnId) {

  public GetConstraintColumnQuery {
    if (constraintColumnId == null || constraintColumnId.isBlank()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "constraintColumnId must not be blank");
    }
  }

}
