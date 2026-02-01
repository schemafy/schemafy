package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetConstraintColumnQuery(String constraintColumnId) {

  public GetConstraintColumnQuery {
    if (constraintColumnId == null || constraintColumnId.isBlank()) {
      throw new InvalidValueException("constraintColumnId must not be blank");
    }
  }

}
