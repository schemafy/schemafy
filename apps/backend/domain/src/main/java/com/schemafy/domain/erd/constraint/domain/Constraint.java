package com.schemafy.domain.erd.constraint.domain;

import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

public record Constraint(
    String id,
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
