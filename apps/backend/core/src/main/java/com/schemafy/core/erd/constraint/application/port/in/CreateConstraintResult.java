package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;

public record CreateConstraintResult(
    String constraintId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
