package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetConstraintColumnsByConstraintIdQuery(String constraintId) {

  public GetConstraintColumnsByConstraintIdQuery {
    if (constraintId == null || constraintId.isBlank()) {
      throw new InvalidValueException("constraintId must not be blank");
    }
  }

}
