package com.schemafy.domain.erd.domain;

import com.schemafy.domain.erd.domain.type.ConstraintKind;

public record Constraint(
    String id,
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
