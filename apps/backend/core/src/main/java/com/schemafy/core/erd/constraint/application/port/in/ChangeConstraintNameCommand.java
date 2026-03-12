package com.schemafy.core.erd.constraint.application.port.in;

public record ChangeConstraintNameCommand(
    String constraintId,
    String newName) {
}
