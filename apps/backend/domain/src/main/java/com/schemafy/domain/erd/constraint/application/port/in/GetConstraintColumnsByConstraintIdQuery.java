package com.schemafy.domain.erd.constraint.application.port.in;

public record GetConstraintColumnsByConstraintIdQuery(String constraintId) {

  public GetConstraintColumnsByConstraintIdQuery {
    if (constraintId == null || constraintId.isBlank()) {
      throw new IllegalArgumentException("constraintId must not be blank");
    }
  }

}
