package com.schemafy.core.erd.constraint.application.port.in;

public record ChangeConstraintDefaultExprCommand(
    String constraintId,
    String defaultExpr) {
}
