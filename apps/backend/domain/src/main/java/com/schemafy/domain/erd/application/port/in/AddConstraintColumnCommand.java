package com.schemafy.domain.erd.application.port.in;

public record AddConstraintColumnCommand(
    String constraintId,
    String columnId,
    int seqNo) {
}
