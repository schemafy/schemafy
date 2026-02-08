package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

public record ConstraintColumnResponse(
    String id,
    String constraintId,
    String columnId,
    int seqNo) {

  public static ConstraintColumnResponse from(ConstraintColumn column) {
    return new ConstraintColumnResponse(
        column.id(),
        column.constraintId(),
        column.columnId(),
        column.seqNo());
  }

}
