package com.schemafy.core.erd.operation.application.inverse;

public record ChangeConstraintDefaultExprInverse(
    String constraintId,
    String oldDefaultExpr) implements InversePayload {

}
