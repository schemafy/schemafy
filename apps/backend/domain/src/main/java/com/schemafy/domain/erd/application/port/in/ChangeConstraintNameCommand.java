package com.schemafy.domain.erd.application.port.in;

public record ChangeConstraintNameCommand(
    String constraintId,
    String newName) {
}
