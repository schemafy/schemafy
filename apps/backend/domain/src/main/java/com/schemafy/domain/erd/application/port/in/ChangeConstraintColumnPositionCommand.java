package com.schemafy.domain.erd.application.port.in;

public record ChangeConstraintColumnPositionCommand(
    String constraintColumnId,
    int seqNo) {
}
