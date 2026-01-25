package com.schemafy.domain.erd.constraint.application.port.in;

public record GetConstraintColumnQuery(String constraintColumnId) {

  public GetConstraintColumnQuery {
    if (constraintColumnId == null || constraintColumnId.isBlank()) {
      throw new IllegalArgumentException("constraintColumnId must not be blank");
    }
  }

}
