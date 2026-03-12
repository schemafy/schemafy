package com.schemafy.core.erd.constraint.application.port.in;

public record ChangeConstraintCheckExprCommand(
    String constraintId,
    String checkExpr) {
}
