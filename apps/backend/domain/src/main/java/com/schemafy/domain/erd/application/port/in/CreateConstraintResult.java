package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.ConstraintKind;

public record CreateConstraintResult(
    String constraintId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
