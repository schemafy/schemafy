package com.schemafy.domain.erd.constraint.application.port.in;

public record GetConstraintQuery(String constraintId) {

  public GetConstraintQuery {
    if (constraintId == null || constraintId.isBlank()) {
      throw new IllegalArgumentException("constraintId must not be blank");
    }
  }

}
