package com.schemafy.domain.erd.constraint.application.port.in;

public record ChangeConstraintDefaultExprCommand(
    String constraintId,
    String defaultExpr) {
}
