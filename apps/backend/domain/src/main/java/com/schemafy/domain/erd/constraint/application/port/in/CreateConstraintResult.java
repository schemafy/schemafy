package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

public record CreateConstraintResult(
    String constraintId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
