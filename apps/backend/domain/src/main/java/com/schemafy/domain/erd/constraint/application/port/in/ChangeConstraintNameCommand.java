package com.schemafy.domain.erd.constraint.application.port.in;

public record ChangeConstraintNameCommand(
    String constraintId,
    String newName) {
}
