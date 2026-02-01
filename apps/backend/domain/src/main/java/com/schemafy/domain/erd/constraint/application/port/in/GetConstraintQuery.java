package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetConstraintQuery(String constraintId) {

  public GetConstraintQuery {
    if (constraintId == null || constraintId.isBlank()) {
      throw new InvalidValueException("constraintId must not be blank");
    }
  }

}
