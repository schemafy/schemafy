package com.schemafy.core.erd.constraint.application.port.in;

public record ChangeConstraintColumnPositionCommand(
    String constraintColumnId,
    int seqNo) {
}
