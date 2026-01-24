package com.schemafy.domain.erd.application.port.in;

public record RemoveConstraintColumnCommand(
    String constraintId,
    String constraintColumnId) {
}
