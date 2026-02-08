package com.schemafy.domain.erd.constraint.application.port.in;

public record ChangeConstraintCheckExprCommand(
    String constraintId,
    String checkExpr) {
}
