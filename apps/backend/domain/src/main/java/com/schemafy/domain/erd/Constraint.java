package com.schemafy.domain.erd;

import com.schemafy.domain.erd.type.ConstraintKind;

public record Constraint(
    String id,
    String tableId,
    String name,
    ConstraintKind kind,
    String checkExpr,
    String defaultExpr) {
}
