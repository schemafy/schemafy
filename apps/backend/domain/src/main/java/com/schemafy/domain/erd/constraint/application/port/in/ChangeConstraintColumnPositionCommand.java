package com.schemafy.domain.erd.constraint.application.port.in;

public record ChangeConstraintColumnPositionCommand(
    String constraintColumnId,
    int seqNo) {
}
