package com.schemafy.api.erd.controller.dto.response;

import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;

public record ConstraintResponse(
    String id,
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {

  public static ConstraintResponse from(CreateConstraintResult result, String tableId) {
    return new ConstraintResponse(
        result.constraintId(),
        tableId,
        result.name(),
        result.kind(),
        result.checkExpr(),
        result.defaultExpr());
  }

  public static ConstraintResponse from(Constraint constraint) {
    return new ConstraintResponse(
        constraint.id(),
        constraint.tableId(),
        constraint.name(),
        constraint.kind(),
        constraint.checkExpr(),
        constraint.defaultExpr());
  }

}
