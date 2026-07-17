package com.schemafy.core.erd.operation.application.inverse;

public record ChangeConstraintCheckExprInverse(
    String constraintId,
    String oldCheckExpr) implements InversePayload {

}
