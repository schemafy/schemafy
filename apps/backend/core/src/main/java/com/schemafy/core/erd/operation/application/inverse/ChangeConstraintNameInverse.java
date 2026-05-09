package com.schemafy.core.erd.operation.application.inverse;

public record ChangeConstraintNameInverse(
    String constraintId,
    String oldName) implements InversePayload {

}
