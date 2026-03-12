package com.schemafy.core.erd.constraint.domain;

import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;

public record Constraint(
    String id,
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
