package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;

public record GetConstraintQuery(String constraintId) {

  public GetConstraintQuery {
    if (constraintId == null || constraintId.isBlank()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "constraintId must not be blank");
    }
  }

}
